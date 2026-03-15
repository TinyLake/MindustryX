package mindustryX.features;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Threads;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import mindustry.net.Packet;
import mindustry.net.Packets.Disconnect;
import mindustry.net.Packets.StreamBegin;
import mindustry.net.Packets.StreamChunk;
import mindustry.net.Packets.WorldStream;
import mindustry.ui.dialogs.BaseDialog;
import mindustryX.VarsX;
import mindustryX.features.SettingsV2.CheckPref;
import mindustryX.features.replay.ReplayCheckpoint;
import mindustryX.features.replay.ReplayKeyframeMeta;
import mindustryX.features.replay.ReplayRecordingSession;
import mindustryX.features.ui.ReplayManagerDialog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static mindustry.Vars.logic;
import static mindustry.Vars.net;
import static mindustry.Vars.netClient;
import static mindustry.Vars.saveDirectory;
import static mindustry.Vars.state;
import static mindustry.Vars.ui;
import static mindustry.Vars.player;
import static mindustryX.features.UIExt.i;

/**
 * 回放录制
 * 原作者 cong0707, 原文件路径 mindustry.arcModule.ReplayController
 * WayZer修改优化
 */
public class ReplayController{
    public static final String extension = "mrep";
    private static final float checkpointInterval = 12f * 60f;
    private static final int maxCheckpoints = 4;
    private static final float checkpointEpsilon = 0.01f;
    private static final CheckPref enable = new CheckPref("replayRecord");
    private static final Object playbackLock = new Object();
    private static final Object serverSessionLock = new Object();

    public static boolean replaying;

    private static ReplayData.Reader reader;
    private static ReplayManagerDialog managerDialog;
    private static ReplayRecordingSession clientSession;
    private static ReplayRecordingSession serverSession;
    private static NetConnection serverSessionOwner;
    private static String pendingServerIp = "unknown";
    private static boolean replayMovementLocked;
    private static boolean previousNoUpdatePlayerMovement;
    private static final Seq<ReplayCheckpoint> checkpoints = new Seq<>();
    private static float playbackClockStart;
    private static float playbackPositionOffset;
    private static float nextCheckpointCaptureOffset;
    private static int playbackRecordOrdinal;
    private static int playbackGeneration;
    private static boolean playbackSuspended;
    private static boolean playbackRestorePending;
    private static float playbackRestoreOffset;

    public static void init(){
        arc.Events.run(EventType.Trigger.update, () -> {
            if(replaying && state.isMenu() && !netClient.isConnecting()){
                stopPlay();
            }
        });
        arc.Events.on(EventType.WorldLoadEvent.class, e -> {
            resetServerMatchReplay();
            completePendingPlaybackRestore();
        });
        arc.Events.on(EventType.ResetEvent.class, e -> resetServerMatchReplay());
        arc.Events.on(EventType.ClientServerConnectEvent.class, e -> {
            stopPlay();
            prepareClientRecording(e.ip + ":" + e.port);
        });
        {
            Table buttons = Vars.ui.join.buttons;
            buttons.button(i("回放管理器"), Icon.file, ReplayController::showManagerDialog);
        }
        {
            var pausedDialog = Vars.ui.paused;
            pausedDialog.shown(() -> {
                if(!replaying){
                    removeReplayPauseControls();
                    return;
                }
                addReplayPauseControls();
            });
        }
    }

    public static void onConnect(String ip){
        prepareClientRecording(ip);
    }

    public static void prepareClientRecording(String ip){
        pendingServerIp = ip == null || ip.isBlank() ? "unknown" : ip;
    }

    public static void onClientWorldDataBegin(){
        if(!recordingEnabled()) return;
        closeClientSession();
        clientSession = openSession("client", pendingServerIp, player == null ? "unknown" : player.name);
        if(clientSession != null){
            Log.info(VarsX.bundle.recording(clientSession.getFile().absolutePath()));
        }
    }

    public static void onClientWorldData(byte[] worldData){
        if(!recordingEnabled() || replaying) return;
        if(clientSession == null){
            onClientWorldDataBegin();
        }
        if(clientSession == null) return;

        try{
            clientSession.recordWorldData(worldData);
            clientSession.recordKeyframeMeta(new ReplayKeyframeMeta(0L, "world-data", 0, worldData.length));
        }catch(Exception e){
            closeClientSession();
            net.disconnect();
            Log.err(e);
            Core.app.post(() -> ui.showException(i("录制出错!"), e));
        }
    }

    public static void onClientPacket(Packet packet){
        if(clientSession == null || replaying) return;
        if(isWorldStreamTransportPacket(packet)) return;
        try{
            clientSession.recordPacket(packet);
            if(packet instanceof Disconnect){
                closeClientSession();
                Log.info(i("录制结束"));
            }
        }catch(Exception e){
            closeClientSession();
            net.disconnect();
            Log.err(e);
            Core.app.post(() -> ui.showException(i("录制出错!"), e));
        }
    }

    public static void onServerWorldData(NetConnection connection, byte[] worldData){
        if(!recordingEnabled() || connection == null || worldData == null) return;
        synchronized(serverSessionLock){
            if(serverSession != null) return;
            String serverIp = connection.address == null || connection.address.isBlank() ? "server" : connection.address;
            String playerName = connection.player == null ? connection.uuid : connection.player.name;
            serverSession = openSession("server", serverIp, playerName);
            if(serverSession != null){
                serverSessionOwner = connection;
            }
            if(serverSession == null) return;

            try{
                serverSession.recordWorldData(worldData);
            }catch(Exception e){
                closeServerSessionLocked();
                Log.err("Failed to start server replay for @.", connection.address, e);
            }
        }
    }

    public static void onServerPacket(NetConnection connection, Packet packet){
        if(isWorldStreamTransportPacket(packet)) return;

        synchronized(serverSessionLock){
            if(serverSession == null) return;
            if(connection != null && connection != serverSessionOwner) return;

            try{
                serverSession.recordPacket(packet);
            }catch(Exception e){
                closeServerSessionLocked();
                Log.err("Failed to record server packet for @.", connection == null ? "server" : connection.address, e);
            }
        }
    }

    public static void onServerDisconnect(NetConnection connection){
        synchronized(serverSessionLock){
            if(serverSession == null || connection == null || connection != serverSessionOwner) return;
            Log.info("Closing server replay after owner @ disconnected.", describeConnection(connection));
            closeServerSessionLocked();
        }
    }

    public static void recordClientKeyframeMeta(long timeline, String tag, int flags, int snapshotSize){
        if(clientSession != null){
            clientSession.recordKeyframeMeta(new ReplayKeyframeMeta(timeline, tag, flags, snapshotSize));
        }
    }

    public static void recordServerKeyframeMeta(NetConnection connection, long timeline, String tag, int flags, int snapshotSize){
        synchronized(serverSessionLock){
            if(serverSession == null) return;
            if(connection != null && connection != serverSessionOwner) return;
            serverSession.recordKeyframeMeta(new ReplayKeyframeMeta(timeline, tag, flags, snapshotSize));
        }
    }

    public static boolean hasClientRecordingSession(){
        return clientSession != null;
    }

    public static boolean hasServerRecordingSession(NetConnection connection){
        synchronized(serverSessionLock){
            return serverSession != null;
        }
    }

    public static void startPlay(Fi input){
        try{
            reader = new ReplayData.Reader(input);
            Log.infoTag("Replay", reader.getMeta().toString());
        }catch(Exception e){
            Core.app.post(() -> {
                showManagerDialog();
                ui.showException(i("读取回放失败!"), e);
            });
            return;
        }

        replaying = true;
        lockReplayPlayerMovement();
        initializePlaybackState();
        ui.loadfrag.show("@connecting");
        ui.loadfrag.setButton(ReplayController::stopPlay);

        logic.reset();
        net.reset();
        netClient.beginConnecting();
        arc.util.Reflect.set(net, "active", true);

        Threads.daemon("Replay Controller", () -> {
            boolean playbackCompleted = false;
            try{
                while(replaying){
                    ReplayData.PlaybackRecord record;
                    ReplayData.Reader activeReader = null;
                    int nextOrdinal;
                    int generation = -1;
                    float clockStart;
                    try{
                        synchronized(playbackLock){
                            if(reader == null){
                                break;
                            }
                            if(playbackSuspended){
                                activeReader = null;
                                record = null;
                                nextOrdinal = playbackRecordOrdinal;
                                generation = playbackGeneration;
                                clockStart = playbackClockStart;
                            }else{
                                generation = playbackGeneration;
                                activeReader = reader;
                                record = activeReader.nextPlaybackRecord();
                                nextOrdinal = playbackRecordOrdinal + 1;
                                clockStart = playbackClockStart;
                            }
                        }
                    }catch(java.io.EOFException e){
                        synchronized(playbackLock){
                            if(reader == null || !replaying){
                                break;
                            }
                            if(playbackSuspended || generation != playbackGeneration || activeReader != reader){
                                continue;
                            }
                            replaying = false;
                            playbackCompleted = true;
                        }
                        break;
                    }

                    if(record == null){
                        Thread.sleep(10L);
                        continue;
                    }

                    while(replaying && generation == currentPlaybackGeneration() && Time.time - clockStart < record.getOffset()){
                        Thread.sleep(1L);
                    }
                    if(!replaying) break;
                    if(generation != currentPlaybackGeneration()) continue;
                    if(!dispatchPlaybackRecord(record, nextOrdinal, generation)) break;
                }
            }catch(Exception e){
                if(replaying){
                    replaying = false;
                    Core.app.post(() -> ui.showException("Replay Error", e));
                }
            }finally{
                if(playbackCompleted){
                    Core.app.post(() -> {
                        if(reader != null){
                            showInfo();
                        }
                        stopPlay();
                    });
                }else{
                    stopPlay();
                }
            }
        });
    }

    public static void stopPlay(){
        boolean wasActive = replaying || reader != null;
        if(wasActive) Log.infoTag("Replay", "stop");
        replaying = false;
        unlockReplayPlayerMovement();
        removeReplayPauseControls();
        ReplayData.Reader closingReader;
        synchronized(playbackLock){
            playbackGeneration++;
            playbackSuspended = false;
            playbackRestorePending = false;
            playbackRestoreOffset = 0f;
            checkpoints.clear();
            playbackRecordOrdinal = 0;
            playbackPositionOffset = 0f;
            nextCheckpointCaptureOffset = checkpointInterval;
            playbackClockStart = 0f;
            closingReader = reader;
            reader = null;
        }
        if(closingReader != null){
            closingReader.close();
        }
        if(!wasActive) return;

        net.disconnect();
        ui.loadfrag.hide();
        Core.app.post(() -> {
            logic.reset();
            showManagerDialog();
        });
    }

    private static void showManagerDialog(){
        if(managerDialog == null) managerDialog = new ReplayManagerDialog();
        managerDialog.show();
    }

    private static void addReplayPauseControls(){
        var pausedDialog = Vars.ui.paused;
        if(pausedDialog.cont.find("ReplayInfo") == null){
            pausedDialog.cont.row();
            pausedDialog.cont.button(i("查看录制信息"), Icon.fileImage, ReplayController::showInfo)
                .name("ReplayInfo")
                .size(0f, 60f)
                .colspan(pausedDialog.cont.getColumns())
                .fill();
        }
        if(pausedDialog.cont.find("ReplayRewind") == null){
            pausedDialog.cont.row();
            pausedDialog.cont.button(VarsX.bundle.replayRewindToCheckpoint(), Icon.undo, ReplayController::rewindToPreviousCheckpoint)
                .name("ReplayRewind")
                .size(0f, 60f)
                .colspan(pausedDialog.cont.getColumns())
                .fill();
        }
    }

    private static void removeReplayPauseControls(){
        var pausedDialog = Vars.ui.paused;
        var info = pausedDialog.cont.find("ReplayInfo");
        if(info != null) info.remove();
        var rewind = pausedDialog.cont.find("ReplayRewind");
        if(rewind != null) rewind.remove();
    }

    public static void showInfo(){
        BaseDialog dialog = new BaseDialog(i("回放统计"));
        if(reader == null){
            dialog.cont.add(i("未加载回放!"));
            dialog.addCloseButton();
            dialog.show();
            return;
        }

        ReplayData replay = reader.getMeta();
        dialog.cont.add(VarsX.bundle.playbackVersion(String.valueOf(replay.getVersion()))).row();
        dialog.cont.add(VarsX.bundle.replayCreationTime(String.valueOf(replay.getTime()))).row();
        dialog.cont.add(VarsX.bundle.serverIp(replay.getServerIp())).row();
        dialog.cont.add(VarsX.bundle.playerName(replay.getRecordPlayer())).row();

        if(reader.getSource() != null){
            ReplayData.Reader tmpReader = new ReplayData.Reader(reader.getSource());
            try{
                Seq<ReplayData.RecordInfo> records = new Seq<>();
                records.addAll(tmpReader.allRecords());
                int packetCount = records.count(r -> r.getType() == ReplayData.RecordType.Packet);
                dialog.cont.add(VarsX.bundle.packetCount(packetCount)).row();
                if(!records.isEmpty()){
                    float maxOffset = 0f;
                    for(ReplayData.RecordInfo record : records){
                        maxOffset = Math.max(maxOffset, record.getOffset());
                    }
                    int secs = (int)(maxOffset / 60f);
                    dialog.cont.add(VarsX.bundle.playbackLength((secs / 3600) + ":" + (secs / 60 % 60) + ":" + (secs % 60))).row();
                }
                dialog.cont.pane(t -> {
                    t.defaults().pad(2f);
                    for(ReplayData.RecordInfo record : records){
                        t.add(Strings.format("+@s", Strings.fixed(record.getOffset() / 60f, 2)));
                        t.add(describeRecord(record)).fillX();
                        t.add("L=" + record.getLength());
                        t.row();
                    }
                }).growX().row();
            }finally{
                tmpReader.close();
            }
        }

        dialog.addCloseButton();
        dialog.show();
    }

    private static boolean recordingEnabled(){
        return enable.get() && !LogicExt.contentsCompatibleMode && !replaying;
    }

    private static ReplayRecordingSession openSession(String prefix, String ip, String playerName){
        var format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
        String fileName = sanitize(prefix + "-" + format.format(new Date())) + "." + extension;
        Fi file = saveDirectory.child(fileName);
        try{
            boolean anonymous = Core.settings.getBool("anonymous", false);
            ReplayData meta = new ReplayData(
                mindustry.core.Version.build,
                new Date(),
                anonymous ? "anonymous" : ip,
                anonymous ? "anonymous" : sanitizePlayerName(playerName)
            );
            return new ReplayRecordingSession(file, new ReplayData.Writer(file.write(false, 8192)), meta);
        }catch(Exception e){
            Log.err(i("创建回放出错!"), e);
            return null;
        }
    }

    private static void handlePlaybackRecord(ReplayData.PlaybackRecord record) throws IOException{
        if(record instanceof ReplayData.PlaybackRecord.Packet packet){
            net.handleClientReceived(packet.getPacket());
        }else if(record instanceof ReplayData.PlaybackRecord.WorldData worldData){
            netClient.worldDataBegin();
            WorldStream data = new WorldStream();
            data.stream = new ByteArrayInputStream(worldData.getData());
            net.handleClientReceived(data);
        }
    }

    private static void closeClientSession(){
        if(clientSession == null) return;
        clientSession.close();
        clientSession = null;
    }

    private static void closeServerSession(){
        synchronized(serverSessionLock){
            closeServerSessionLocked();
        }
    }

    private static void resetServerMatchReplay(){
        closeServerSession();
    }

    private static boolean isWorldStreamTransportPacket(Packet packet){
        return packet instanceof WorldStream
            || packet instanceof StreamBegin
            || packet instanceof StreamChunk
            || isWorldDataBeginControlPacket(packet);
    }

    private static boolean isWorldDataBeginControlPacket(Packet packet){
        if(packet == null) return false;
        String className = packet.getClass().getName();
        return className.equals("mindustry.gen.WorldDataBeginCallPacket")
            || className.endsWith(".WorldDataBeginCallPacket")
            || packet.getClass().getSimpleName().equals("WorldDataBeginCallPacket");
    }

    private static String describeConnection(NetConnection connection){
        if(connection == null) return "server";
        if(connection.player != null && connection.player.name != null && !connection.player.name.isBlank()){
            return connection.player.name;
        }
        if(connection.address != null && !connection.address.isBlank()){
            return connection.address;
        }
        return connection.uuid == null || connection.uuid.isBlank() ? "unknown" : connection.uuid;
    }

    private static boolean dispatchPlaybackRecord(ReplayData.PlaybackRecord record, int nextOrdinal, int generation) throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        Core.app.post(() -> {
            try{
                if(replaying && generation == currentPlaybackGeneration()){
                    handlePlaybackRecord(record);
                    synchronized(playbackLock){
                        if(generation == playbackGeneration){
                            playbackRecordOrdinal = nextOrdinal;
                            playbackPositionOffset = record.getOffset();
                            captureCheckpointIfDue(record.getOffset(), nextOrdinal);
                        }
                    }
                }
            }catch(Exception e){
                failure.set(e);
                replaying = false;
                net.handleException(e);
            }finally{
                latch.countDown();
            }
        });

        while(!latch.await(10L, TimeUnit.MILLISECONDS)){
            if(!replaying){
                break;
            }
        }
        return replaying && failure.get() == null;
    }

    private static void lockReplayPlayerMovement(){
        if(replayMovementLocked) return;
        previousNoUpdatePlayerMovement = LogicExt.noUpdatePlayerMovement;
        LogicExt.noUpdatePlayerMovement = true;
        replayMovementLocked = true;
    }

    private static void unlockReplayPlayerMovement(){
        if(!replayMovementLocked) return;
        LogicExt.noUpdatePlayerMovement = previousNoUpdatePlayerMovement;
        replayMovementLocked = false;
    }

    private static String sanitizePlayerName(String name){
        String trimmed = name == null ? "unknown" : name.trim();
        return trimmed.isEmpty() ? "unknown" : trimmed;
    }

    private static String sanitize(String value){
        return Strings.sanitizeFilename(value == null || value.isBlank() ? "replay" : value);
    }

    private static String describeRecord(ReplayData.RecordInfo record){
        return switch(record.getType()){
            case Packet -> Net.newPacket(record.getPacketId()).getClass().getSimpleName();
            case WorldData -> "WorldData";
            case KeyframeMeta -> "KeyframeMeta";
        };
    }

    private static void initializePlaybackState(){
        synchronized(playbackLock){
            checkpoints.clear();
            playbackRecordOrdinal = 0;
            playbackPositionOffset = 0f;
            nextCheckpointCaptureOffset = checkpointInterval;
            playbackClockStart = Time.time;
            playbackGeneration++;
            playbackSuspended = false;
            playbackRestorePending = false;
            playbackRestoreOffset = 0f;
        }
    }

    private static int currentPlaybackGeneration(){
        synchronized(playbackLock){
            return playbackGeneration;
        }
    }

    private static void suspendPlaybackAt(float offset){
        playbackPositionOffset = offset;
        playbackClockStart = 0f;
        playbackGeneration++;
        playbackSuspended = true;
    }

    private static void resumePlaybackAt(float offset){
        playbackGeneration++;
        playbackPositionOffset = offset;
        playbackClockStart = Time.time - offset;
        playbackSuspended = false;
    }

    private static void captureCheckpointIfDue(float offset, int nextOrdinal){
        if(offset + checkpointEpsilon < nextCheckpointCaptureOffset) return;
        if(reader == null || reader.getSource() == null || player == null || !state.isGame()) return;

        try{
            ReplayCheckpoint checkpoint = ReplayCheckpoint.capture(offset, nextOrdinal);
            ReplayCheckpoint existing = checkpoints.isEmpty() ? null : checkpoints.peek();
            if(existing != null && existing.getNextRecordOrdinal() == checkpoint.getNextRecordOrdinal()){
                checkpoints.set(checkpoints.size - 1, checkpoint);
            }else{
                checkpoints.add(checkpoint);
                while(checkpoints.size > maxCheckpoints){
                    checkpoints.remove(0);
                }
            }
            nextCheckpointCaptureOffset = checkpoint.getOffset() + checkpointInterval;
        }catch(Exception e){
            Log.err("Failed to capture replay checkpoint at @.", offset, e);
        }
    }

    private static void rewindToPreviousCheckpoint(){
        ReplayCheckpoint checkpoint;
        Fi source;
        synchronized(playbackLock){
            float suspendedOffset = currentReplayOffset();
            checkpoint = findPreviousCheckpoint(suspendedOffset);
            if(checkpoint == null){
                UIExt.announce(VarsX.bundle.replayNoCheckpoint());
                return;
            }
            source = reader == null ? null : reader.getSource();
            suspendPlaybackAt(suspendedOffset);
        }
        if(source == null){
            synchronized(playbackLock){
                if(replaying){
                    resumePlaybackAt(playbackPositionOffset);
                }else{
                    playbackSuspended = false;
                }
            }
            UIExt.announce(VarsX.bundle.replayNoCheckpoint());
            return;
        }

        ui.paused.hide();
        ui.loadfrag.show("@connecting.data");
        ui.loadfrag.setButton(ReplayController::stopPlay);

        ReplayCheckpoint target = checkpoint;
        Threads.daemon("Replay Rewind", () -> {
            ReplayData.Reader replacement = null;
            try{
                replacement = reopenReader(source, target.getNextRecordOrdinal());
                ReplayData.Reader finalReplacement = replacement;
                replacement = null;
                Core.app.post(() -> finishRewind(target, finalReplacement));
            }catch(Exception e){
                ReplayData.Reader toClose = replacement;
                Core.app.post(() -> {
                    if(toClose != null){
                        toClose.close();
                    }
                    synchronized(playbackLock){
                        if(replaying){
                            resumePlaybackAt(playbackPositionOffset);
                        }else{
                            playbackSuspended = false;
                        }
                    }
                    ui.loadfrag.hide();
                    if(replaying){
                        ui.showException("Replay Rewind Error", e);
                    }
                });
            }
        });
    }

    private static ReplayData.Reader reopenReader(Fi source, int nextOrdinal) throws IOException{
        ReplayData.Reader replacement = new ReplayData.Reader(source);
        boolean success = false;
        try{
            for(int i = 0; i < nextOrdinal; i++){
                replacement.nextPlaybackRecord();
            }
            success = true;
            return replacement;
        }finally{
            if(!success){
                replacement.close();
            }
        }
    }

    private static void finishRewind(ReplayCheckpoint checkpoint, ReplayData.Reader replacement){
        ReplayData.Reader previousReader = null;
        boolean replacementInstalled = false;
        try{
            synchronized(playbackLock){
                if(!replaying){
                    replacement.close();
                    playbackSuspended = false;
                    return;
                }
                previousReader = reader;
                reader = replacement;
                replacementInstalled = true;
                playbackRecordOrdinal = checkpoint.getNextRecordOrdinal();
                playbackPositionOffset = checkpoint.getOffset();
                playbackRestorePending = true;
                playbackRestoreOffset = checkpoint.getOffset();
                trimCheckpointsAfter(checkpoint.getOffset());
                nextCheckpointCaptureOffset = checkpoint.getOffset() + checkpointInterval;
            }
            if(previousReader != null && previousReader != replacement){
                previousReader.close();
                previousReader = null;
            }

            handlePlaybackRecord(new ReplayData.PlaybackRecord.WorldData(checkpoint.getWorldData()));
        }catch(Exception e){
            if(previousReader != null && previousReader != replacement){
                previousReader.close();
            }
            synchronized(playbackLock){
                if(replacementInstalled && reader == replacement){
                    reader = null;
                }
                playbackSuspended = false;
                playbackRestorePending = false;
                playbackRestoreOffset = 0f;
            }
            replacement.close();
            ui.loadfrag.hide();
            if(replaying){
                ui.showException("Replay Rewind Error", e);
            }
            stopPlay();
        }
    }

    private static float currentReplayOffset(){
        return Math.max(playbackPositionOffset, playbackClockStart == 0f ? playbackPositionOffset : Time.time - playbackClockStart);
    }

    private static ReplayCheckpoint findPreviousCheckpoint(float currentOffset){
        for(int i = checkpoints.size - 1; i >= 0; i--){
            ReplayCheckpoint checkpoint = checkpoints.get(i);
            if(checkpoint.getOffset() < currentOffset - checkpointEpsilon){
                return checkpoint;
            }
        }
        return null;
    }

    private static void trimCheckpointsAfter(float offset){
        while(!checkpoints.isEmpty() && checkpoints.peek().getOffset() > offset + checkpointEpsilon){
            checkpoints.pop();
        }
    }

    private static void completePendingPlaybackRestore(){
        synchronized(playbackLock){
            if(!playbackRestorePending){
                return;
            }
            if(!replaying || reader == null){
                playbackRestorePending = false;
                playbackRestoreOffset = 0f;
                return;
            }
            float restoreOffset = playbackRestoreOffset;
            playbackRestorePending = false;
            playbackRestoreOffset = 0f;
            resumePlaybackAt(restoreOffset);
        }
        state.set(State.playing);
        ui.loadfrag.hide();
    }

    private static void closeServerSessionLocked(){
        serverSessionOwner = null;
        if(serverSession == null) return;
        serverSession.close();
        serverSession = null;
    }
}
