package mindustryX.features.replay;

public class ReplayKeyframeMeta{
    private final long timeline;
    private final String tag;
    private final int flags;
    private final int snapshotSize;

    public ReplayKeyframeMeta(long timeline, String tag, int flags, int snapshotSize){
        this.timeline = timeline;
        this.tag = tag == null ? "" : tag;
        this.flags = flags;
        this.snapshotSize = snapshotSize;
    }

    public long getTimeline(){
        return timeline;
    }

    public String getTag(){
        return tag;
    }

    public int getFlags(){
        return flags;
    }

    public int getSnapshotSize(){
        return snapshotSize;
    }
}
