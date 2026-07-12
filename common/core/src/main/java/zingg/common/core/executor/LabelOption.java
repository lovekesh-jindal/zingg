package zingg.common.core.executor;

import java.util.Optional;
import zingg.common.client.util.ColValues;

/*
The set of choices a user can make while labelling a pair used to be encoded as scattered magic number: the literal QUIT value {@code9} and the regex
{@code "[0129]"} in @code.labeller that had to be kept in sync by hand with @link Colvalues match-type code . That duplication could silently drift and it
conflated a data label with control action

This enum make the choice a single-source type 
*/
public enum LabelOption{
  NO_MATCH(ColValues.MATCH_TYPE_NOT_A_MATCH),
  MATCH(ColValues.MATCH_TYPE_MATCH),
  NOT_SURE(ColValues.MATCH_TYPE_NOT_SURE),
  QUIT(9);

  private final int code;

  LabelOption(int code){
    this.code = code;
  }

  public static Optional<LabelOption> fromCode(int code){
    for(LabelOption option : values()) {
      if(option.code == code){
        return Optional.of(option);
      }
    }
    return Optional.empty();
  }
  
}
