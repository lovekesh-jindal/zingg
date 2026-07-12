package zingg.common.client;

//TODO need to revisit this interface
public interface IZinggModelInfo<S,D,R,C> {
    ZFrame<D,R,C>  getMarkedRecords();
   // Declares throws ZinggClientException so that implementation can propagate a genuine read failure to the caller instead of swallowing it and returning null
    ZFrame<D,R,C>  getUnmarkedRecords() throws ZinggClientException;

    Long getMarkedRecordsStat(ZFrame<D, R, C> markedRecords, long value);

    Long getMatchedMarkedRecordsStat(ZFrame<D, R, C> markedRecords);

    Long getUnmatchedMarkedRecordsStat(ZFrame<D, R, C> markedRecords);

    Long getUnsureMarkedRecordsStat(ZFrame<D, R, C> markedRecords);

    ITrainingDataModel<S, D, R, C> getTrainingDataModel();

}
