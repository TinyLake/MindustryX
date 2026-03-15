package mindustryX.features.replay;

import arc.files.Fi;
import arc.util.Log;
import mindustry.net.Packet;
import mindustryX.features.ReplayData;

public class ReplayRecordingSession implements AutoCloseable{
    private final Fi file;
    private final ReplayData.Writer writer;
    private boolean closed;

    public ReplayRecordingSession(Fi file, ReplayData.Writer writer, ReplayData meta){
        this.file = file;
        this.writer = writer;
        writer.writeHeader(meta);
    }

    public Fi getFile(){
        return file;
    }

    public synchronized void recordPacket(Packet packet){
        if(closed) return;
        writer.writePacket(packet);
    }

    public synchronized void recordWorldData(byte[] data){
        if(closed) return;
        writer.writeWorldData(data);
    }

    public synchronized void recordKeyframeMeta(ReplayKeyframeMeta meta){
        if(closed) return;
        writer.writeKeyframeMeta(meta);
    }

    @Override
    public synchronized void close(){
        if(closed) return;
        closed = true;
        try{
            writer.close();
        }catch(Exception e){
            Log.err("Failed to close replay recording '@'.", file.absolutePath(), e);
        }
    }
}
