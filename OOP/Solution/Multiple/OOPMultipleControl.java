package OOP.Solution.Multiple;

import OOP.Provided.Multiple.OOPCoincidentalAmbiguity;
import OOP.Provided.Multiple.OOPInherentAmbiguity;
import OOP.Provided.Multiple.OOPMultipleException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

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

    public int classDistance(Class source, Class dest){
        int delta = 0;
        String destName = dest.getName();
        for(Class c = source ; c.getName() != destName ; c = c.getSuperclass()){
            delta++;
        }
        return delta;
    }

    public int distance(Class[] sourceTypes, Class[] destTypes){
        int dist = 0;
        for(int i =0 ; i< destTypes.length ; i++){
            Class currSource = sourceTypes[i];
            Class currDest = destTypes[i];
            if (!currDest.getName().equals(currSource.getName())){
                dist+=classDistance(currSource,currDest);
            }
        }
        return dist;
    }

    public List<Method> canInvoke(Class[] parameters,
                          String methodName, Class klass) {
        List<Method> retMethods  = new LinkedList<Method>();
        for (Method method : klass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != parameters.length) continue;
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(parameters[i]
                        .getClass())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                retMethods.add(method);
            }
        }
        return retMethods;
    }

    List<Method> checkInInterfaceClass(Class in, String methodName, Class[] argsClass)
            throws NoSuchMethodException{
        String inName = in.getName();
        StringBuilder klassName = new StringBuilder(inName);
        klassName.setCharAt(inName.length()-2, 'C');
        Method me;
        Class klass;
        try{
            klass = Class.forName(klassName.toString());
        }catch(Exception e){ throw new NoSuchMethodException();}
        return canInvoke(argsClass,methodName,klass);
    }

    //TODO: fill in here :
    public Object invoke(String methodName, Object[] args) throws OOPMultipleException {
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(interfaceClass.getInterfaces()));
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new LinkedList<>();
        ArrayList<Pair<Class<?>, Method>> candidates = new ArrayList<>();
        List<Method> matches = new LinkedList<Method>();
        if(args == null) args = new Object[0];
        Class[] cArgs = Arrays.stream(args).map(arg -> arg.getClass()).toArray(Class[]::new);

        while (!interfaces.isEmpty()){
            for (Class in : interfaces){
                try {
                    matches.addAll(checkInInterfaceClass(in,methodName,cArgs));
                }
                catch(NoSuchMethodException e){}
                newInterfaces.addAll(Arrays.asList(in.getInterfaces()));
                visited.add(in);
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
        int min = -1;
        Method toInvoke = null;
        for(Method met : matches){
            int dist = distance(cArgs,met.getParameterTypes());
            if (dist == min){
                throw new OOP.Provided.Multiple.OOPCoincidentalAmbiguity(candidates);
            }
            if(min == -1 || dist < min){
                min = dist;
                toInvoke = met;
            }
        }
        Object ret = null;
        try{
            ret = toInvoke.invoke(toInvoke.getDeclaringClass().newInstance(),args);
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
