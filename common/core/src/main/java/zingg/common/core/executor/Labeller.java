package zingg.common.core.executor;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import zingg.common.client.ILabelDataViewHelper;
import zingg.common.client.ITrainingDataModel;
import zingg.common.client.ZFrame;
import zingg.common.client.ZinggClientException;
import zingg.common.client.cols.ZidAndFieldDefSelector;
import zingg.common.client.options.ZinggOptions;
import zingg.common.client.util.ColName;
import zingg.common.client.util.DFObjectUtil;
import zingg.common.core.preprocess.IPreprocessors;
import zingg.common.core.util.LabellerUtil;

public abstract class Labeller<S,D,R,C,T> extends ZinggBase<S,D,R,C,T> implements IPreprocessors<S,D,R,C,T> {
// DESIGN CHANGE : the quit code is now sourced from LabelOption instead of a bare magic literal , so the value lives in exactly one place.
	public static final Integer QUIT_LABELING = LabelOption.QUIT.code();
	public static final Integer INCREMENT = 1;
	private static final long serialVersionUID = 1L;
	protected static String name = "zingg.common.core.executor.Labeller";
	public static final Log LOG = LogFactory.getLog(Labeller.class);
	protected ITrainingDataModel<S, D, R, C> trainingDataModel;
	protected ILabelDataViewHelper<S, D, R, C> labelDataViewHelper;
	protected LabelUserInput labelUserInput;
	
	public Labeller() {
		setZinggOption(ZinggOptions.LABEL);
	}

	public void execute() throws ZinggClientException {
		try {
			LabellerUtil<D, R, C> labellerUtil = new LabellerUtil<D, R, C>();
			LOG.info("Reading inputs for labelling phase ...");
			getTrainingDataModel().setMarkedRecordsStat(getMarkedRecords());
			ZFrame<D,R,C>  unmarkedRecords = getUnmarkedRecords();
			ZFrame<D, R, C> preprocessedUnmarkedRecords = preprocess(unmarkedRecords);
			ZFrame<D,R,C>  updatedLabelledRecords = processRecordsCli(preprocessedUnmarkedRecords);
			//only post processing if there are labelled records
			if(updatedLabelledRecords != null){
				ZFrame<D, R, C> postProcessedLabelledRecords = labellerUtil.postProcessLabel(updatedLabelledRecords, unmarkedRecords);
				getTrainingDataModel().writeLabelledOutput(postProcessedLabelledRecords,args);
			}
			LOG.info("Finished labelling phase");
		} catch (Exception e) {
			throw new ZinggClientException("Error in labelling phase ", e);
		}
	}

// (Design change) ZinggClientException is now a subtype of Exception , so this single catch( Exception) already handle it as well as ny runtime error	
// The previous version caught every exception , logged a misleading "No marked record". That disguised real failure like bad path , expired credentias and user 
// was silently told that there was nothing to label while storage was actually broken 	
	public ZFrame<D,R,C> getUnmarkedRecords() throws ZinggClientException {
		ZFrame<D,R,C> unmarkedRecords = getPipeUtil().read(false, false, getModelHelper().getTrainingDataUnmarkedPipe(args));
		ZFrame<D,R,C> markedRecords = readMarkedRecordsOrNull();

			
			if (markedRecords != null ) {
				unmarkedRecords = unmarkedRecords.join(markedRecords,ColName.CLUSTER_COLUMN, false,
						"left_anti");
				getTrainingDataModel().setMarkedRecordsStat(markedRecords);
			} 
		
	
		return unmarkedRecords;
	}

	public ZFrame<D,R,C> readMarkedRecordsOrNull(){
		try{
			return getPipeUtil().read(false, false, getModelHelper().getTrainingDataMarkedPipe(args));
		}catch(ZinggClientException e){
			LOG.info("No marked records yet - treating as first labelling run.");
			return null;
		}
	}
/*
DESIGN CHANGE :  method level SINGLE RESPONSIBILTY PRINCIPLE 
*/
	public ZFrame<D,R,C> processRecordsCli(ZFrame<D,R,C>  lines) throws ZinggClientException {
		LOG.info("Processing Records for CLI Labelling");
		if (lines != null && lines.count() > 0) {
		LOG.info("It seems there are no unmarked records at this moment. Please run findTrainingData Job to build some pairs to be labelled and then run this labeler.");
		return null;	
		);
        printCurrentStats();
		lines = lines.cache();
		ZidAndFieldDefSelector zidAndFieldDefSelector = new ZidAndFieldDefSelector(args.getFieldDefinition(), false, args.getShowConcise());
		//have to introduce as snowframe can not handle row.getAs with column
		//name and row and lines are out of order for the code to work properly
		//snow getAsString expects row to have same struc as dataframe which is 
		//not happening
		ZFrame<D,R,C> clusterIdZFrame = getLabelDataViewHelper().getClusterIdsFrame(lines);
		List<R>  clusterIDs = getLabelDataViewHelper().getClusterIds(clusterIdZFrame);
		try {
	        return labelAllPairs(lines, clustedIDs, clusterIdZFrame, zidAndFieldDefSelector);
			} catch (Exception e) {
				LOG.error("Labelling error has occurred ", e);
				throw new ZinggClientException("An error has occured while Labelling.", e);
			}
		} else {
			LOG.info("It seems there are no unmarked records at this moment. Please run findTrainingData job to build some pairs to be labelled and then run this labeler.");
			return null;
		}
	}

	private ZFrame<D,R,C> labelAllPairs( ZFrame<D,R,C> lines, List<R> clustedIDs, ZFrame<D,R,C> clusterIdZFrame, ZidAndFieldDefSelector zidAndFieldDefSelector){
		int totalPairs  = clustedIDs.size();
		ZFrame<D,R,C> updatedRecords = null;
		for(int index = 0; index < totalPairs , index++){
			ZFrame<D,R,C> currentPair = getLabelDataViewHelper().getCurrentPair(lines, index, clustedIDs, clusterIdZFrame);
			int selectedOption = labelSinglePair(currentPair, index, totalPairs, zidAndFieldDefSelector);
			if(selectedOption == QUIT_LABELING){
				LOG.info("User has quit in the middle. Updating the records.");
				break;
			}
			updatedRecords = recordAnswer(selectedOption, currentPair , updatedRecords);
		}
		LOG.info("Processing finised.");
		return updatedRecords;
	}
   	private int labelSinglePair(ZFrame<D,R,C> currentPair, int index, int totalPairs, ZidAndFieldDefSelector zidAndFieldDefSelector) throws ZinggClientException{
		double score  = getLabelDataViewHelper().getScore(currentPair);
		double prediction = getLabelDataViewHelper().getPrediction(currentPair);
		String msg1 = getLabelDataViewHelper().getMsg1(index, totalPairs);
		String msg2 = getLabelDataViewHelper().getMsg2(prediction, score);
		return displayRecordsAndGetUserInput(currentPair.select(zidAndFieldDefSelector.getCols()), msg1, msg2);
	}
	private ZFrame<D,R,C> recordAnswer(int selectedOption,  ZFrame<D,R,C> currentPair, ZFrame<D,R,C> updatedRecords){
		getTrainingDataModel().updateLabellerStat(selectedOption, INCREMENT);
		printCurrentStats();
		return getTrainingDataModel().updateRecords(selectedOption, currentPair, updatedRecords);
	}
	private void printCurrentStats(){
		getLabelDataViewHelper().printMarkedRecordsStat(
					getTrainingDataModel().getPositivePairsCount(),
					getTrainingDataModel().getNegativePairsCount(),
					getTrainingDataModel().getNotSurePairsCount(),
					getTrainingDataModel().getTotalCount()
			);
	}
	protected int displayRecordsAndGetUserInput(ZFrame<D,R,C> records, String preMessage, String postMessage) throws ZinggClientException {
		getLabelDataViewHelper().displayRecords(records, preMessage, postMessage);
		return readCliInput().code();
	}

	public LabelUserInput getLabelUserInput(){
		if(labelUserInput == null){
			labelUserInput = new CliLabelUserInput();
		}
		return labelUserInput;
	}
	
	public void setLabelUserInput(LabelUserInput labelUserInput){
		this.labelUserInput = labelUserInput;
	}

// DESIGN CHANGE : return a typed LabelOption instead of a bare int and derives the set of valid inputs from the enum rather than the hand maintained
// "[0129]" regex , so the allowed options cannot drift out of sync 	
// here we are decoupling the labelling loop from the console 
	LabelOption readCliInput() {
		return getLabelUserInput().getUserSelection();
	}

	@Override
	public ITrainingDataModel<S, D, R, C> getTrainingDataModel() {	
		if (trainingDataModel==null) {
			this.trainingDataModel = new TrainingDataModel<S, D, R, C, T>(getContext(), getClientOptions());
		}
		return trainingDataModel;
    }

	public void setTrainingDataModel(ITrainingDataModel<S, D, R, C> trainingDataModel) {
		this.trainingDataModel = trainingDataModel;
	}

	
	public ILabelDataViewHelper<S, D, R, C> getLabelDataViewHelper() {
		if(labelDataViewHelper==null) {
			labelDataViewHelper = new LabelDataViewHelper<S,D,R,C,T>(getContext(), getClientOptions());
			labelDataViewHelper.initVerticalDisplayUtility(getDfObjectUtil());
		}
    	return labelDataViewHelper;
    }

	public void setLabelDataViewHelper(ILabelDataViewHelper<S, D, R, C> labelDataViewHelper) {
		this.labelDataViewHelper = labelDataViewHelper;
	}

	protected abstract DFObjectUtil<S, D, R, C> getDfObjectUtil();
}


