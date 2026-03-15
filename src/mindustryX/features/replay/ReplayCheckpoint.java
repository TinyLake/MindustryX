package mindustryX.features.replay;

import arc.util.io.FastDeflaterOutputStream;
import mindustry.Vars;
import mindustry.net.NetworkIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReplayCheckpoint{
    private final byte[] worldData;
    private final float offset;
    private final int nextRecordOrdinal;

    public ReplayCheckpoint(byte[] worldData, float offset, int nextRecordOrdinal){
        this.worldData = worldData;
        this.offset = offset;
        this.nextRecordOrdinal = nextRecordOrdinal;
    }

    public byte[] getWorldData(){
        return worldData;
    }

    public float getOffset(){
        return offset;
    }

    public int getNextRecordOrdinal(){
        return nextRecordOrdinal;
    }

    public static ReplayCheckpoint capture(float offset, int nextRecordOrdinal) throws IOException{
        ByteArrayOutputStream output = new ByteArrayOutputStream(32768);
        try(FastDeflaterOutputStream deflater = new FastDeflaterOutputStream(output)){
            NetworkIO.writeWorld(Vars.player, deflater);
        }
        return new ReplayCheckpoint(output.toByteArray(), offset, nextRecordOrdinal);
    }
}
