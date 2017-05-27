package OOP.Solution.Multiple;

import OOP.Provided.Multiple.OOPInherentAmbiguity;
import OOP.Provided.Multiple.OOPMultipleException;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import OOP.Provided.Multiple.OOPBadClass;

public class OOPMultipleControl {

    //TODO: DO NOT CHANGE !!!!!!
    private Class<?> interfaceClass;
    private File sourceFile;

    //TODO: DO NOT CHANGE !!!!!!
    public OOPMultipleControl(Class<?> interfaceClass, File sourceFile) {
        this.interfaceClass = interfaceClass;
        this.sourceFile = sourceFile;
    }

    //TODO: fill in here :
    public void validateInheritanceGraph() throws OOPMultipleException {

        List<Class> interfaces = Arrays.asList(interfaceClass.getInterfaces());
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new ArrayList<>();

        while (!interfaces.isEmpty()){
            for (Class i : interfaces) {
                if (!Arrays.asList(i.getAnnotations()).contains(OOPMultipleInterface.class)){
                    throw new OOPBadClass(i);
                }
                for(Method m : i.getDeclaredMethods()){
                    if (!Arrays.asList(m.getAnnotations()).contains(OOPMultipleMethod.class)){
                        throw new OOPBadClass(m);
                    }
                }
                if (visited.contains(i) && i.getMethods().length != 0) {
                    throw new OOPInherentAmbiguity(interfaceClass, i, i.getMethods()[0]);
                }
                newInterfaces.addAll(Arrays.asList(i.getInterfaces()));
                visited.add(i);
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }


    }

    //TODO: fill in here :
    public Object invoke(String methodName, Object[] args)
            throws OOPMultipleException {
        return null;
    }

    //TODO: add more of your code :


    //TODO: DO NOT CHANGE !!!!!!
    public void removeSourceFile() {
        if (sourceFile.exists()) {
            sourceFile.delete();
        }
    }
}
