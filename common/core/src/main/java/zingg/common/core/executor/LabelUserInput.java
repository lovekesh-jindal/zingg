package zingg.common.core.executor;

/*
DESIGN ISSUE :  separate the labelling logic from console I/O.
*/
public interface LabelUserInput{

  LabelOption getUserSelection();

  
}
