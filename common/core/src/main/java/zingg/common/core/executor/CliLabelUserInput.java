package zingg.common.core.executor;

import java.util.Optional;
import java.util.Scanner;

/*
DESIGN CHANGE :  default console adapter for LabelUserInput
*/
public class CliLabelUserInput implements LabelUserInput {

   private final Scanner scanner;

  public CliLabelUserInput(){
    this(new Scanner(System.in));
  }

  public CliLabelUserInput(Scanner scanner){
    this.scanner = scanner;
  }

  @Override
  public LabelOption getUserSelection(){
    while (true) {
			Optional<LabelOption> option = Optional.empty();
			if(scanner.hasNextInt()){
				option = LabelOption.fromCode(scanner.nextInt());
			}else{
				scanner.next();
			}
			if(option.isPresent()){
				return option.get();
			}
			System.out.println("Nope, please enter one of the allowed options!");
		}
  }
}
