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

        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(interfaceClass.getInterfaces()));
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new LinkedList<>();

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

    Pair<Class<?>, Method> checkInInterfaceClass(Class in, String methodName, Class[] argsClass)
            throws NoSuchMethodException{
        String inName = in.getName();
        StringBuilder klassName = new StringBuilder(inName);
        klassName.setCharAt(inName.length()-2, 'C');
        Method me;
        Class klass;
        try{
            klass = Class.forName(klassName.toString());
        }catch(Exception e){ throw new NoSuchMethodException();}
        try{
            me = klass.getMethod(methodName,argsClass);
        } catch(Exception e){ throw new NoSuchMethodException();}
        return new Pair(klass,me);
    }

    //TODO: fill in here :
    public Object invoke(String methodName, Object[] args) throws OOPMultipleException {
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(interfaceClass.getInterfaces()));
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new LinkedList<>();
        ArrayList<Pair<Class<?>, Method>> candidates = new ArrayList<>();
        if(args == null) args = new Object[0];
        Class[] cArgs = Arrays.stream(args).map(arg -> arg.getClass()).toArray(Class[]::new);;

        while (!interfaces.isEmpty()){
            for (Class in : interfaces){
                try {
                    Pair p = checkInInterfaceClass(in,methodName,cArgs);
                    candidates.add(p);
                }
                catch(NoSuchMethodException e){}
                newInterfaces.addAll(Arrays.asList(in.getInterfaces()));
                visited.add(in);
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
        if (candidates.size() > 1 || candidates.size() == 0){
            throw new OOP.Provided.Multiple.OOPCoincidentalAmbiguity(candidates);
        }
        Object ret = null;
        Pair p = candidates.get(0);
        Method methodToInvoke = (Method)p.getValue();
        Class classToInvoke = (Class)p.getKey();
        try{
            ret = methodToInvoke.invoke(classToInvoke.newInstance(),args);
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
