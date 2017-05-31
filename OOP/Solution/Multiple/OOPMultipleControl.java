package OOP.Solution.Multiple;

import OOP.Provided.Multiple.OOPCoincidentalAmbiguity;
import OOP.Provided.Multiple.OOPInherentAmbiguity;
import OOP.Provided.Multiple.OOPMultipleException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import OOP.Provided.Multiple.OOPBadClass;
import javafx.util.Pair;

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
                if (!i.isAnnotationPresent(OOPMultipleInterface.class)){
                    throw new OOPBadClass(i);
                }
                for(Method m : i.getDeclaredMethods()){
                    if (!m.isAnnotationPresent(OOPMultipleMethod.class)){
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
    public Object invoke(String methodName, Object[] args) throws OOPMultipleException {
        List<Class> interfaces = Arrays.asList(interfaceClass.getInterfaces());
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new ArrayList<>();
        ArrayList<Pair<Class<?>, Method>> candidates = new ArrayList<>();
        Class[] cArgs = Arrays.stream(args).map(arg -> arg.getClass()).toArray(Class[]::new);;

        while (!interfaces.isEmpty()){
            for (Class klass : interfaces){
                try {
                    Method invokedMethod = klass.getDeclaredMethod(methodName, cArgs);
                    Pair p = new Pair(klass,invokedMethod);
                    candidates.add(p);
                }
                catch(NoSuchMethodException e){}
                newInterfaces.addAll(Arrays.asList(klass.getInterfaces()));
                visited.add(klass);
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
        if (candidates.size() > 1){
            throw new OOP.Provided.Multiple.OOPCoincidentalAmbiguity(candidates);
        }
        Object ret = null;
        Pair p = candidates.get(0);
        Method methodToInvoke = (Method)p.getValue();
        Class classToInvoke = (Class)p.getKey();
        try{
            ret = methodToInvoke.invoke(classToInvoke,args);
        }catch (Exception e){ throw new OOPCoincidentalAmbiguity(candidates);}

        return ret;
    }

    //TODO: add more of your code :


    //TODO: DO NOT CHANGE !!!!!!
    public void removeSourceFile() {
        if (sourceFile.exists()) {
            sourceFile.delete();
        }
    }
}
