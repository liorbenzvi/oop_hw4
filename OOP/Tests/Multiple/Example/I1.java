package OOP.Tests.Multiple.Example;

import OOP.Provided.Multiple.OOPMultipleException;
import OOP.Solution.Multiple.OOPMultipleInterface;
import OOP.Solution.Multiple.OOPMultipleMethod;

@OOPMultipleInterface
public interface I1 {

    @OOPMultipleMethod
    String f() throws OOPMultipleException;
}
