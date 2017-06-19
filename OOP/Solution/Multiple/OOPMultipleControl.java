package OOP.Solution.Multiple;

import OOP.Provided.Multiple.OOPInherentAmbiguity;
import OOP.Provided.Multiple.OOPMultipleException;
import java.io.File;
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

        List<Class> interfaces = new LinkedList<>(Arrays.asList(interfaceClass.getInterfaces()));
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new LinkedList<>();
        HashSet<Method> ImplMethods = new HashSet<>();

        while (!interfaces.isEmpty()) {
            for (Class i : interfaces) {
                if (!i.isAnnotationPresent(OOPMultipleInterface.class)){
                    throw new OOPBadClass(i);
                }
                for(Method m : i.getDeclaredMethods()){
                    if (!m.isAnnotationPresent(OOPMultipleMethod.class)){
                        throw new OOPBadClass(m);
                    }
                }
                newInterfaces.addAll(Arrays.asList(i.getInterfaces()));
                visited.add(i);
                ImplMethods.addAll(Arrays.asList(i.getDeclaredMethods()));
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }

        interfaces = new LinkedList<>(Arrays.asList(interfaceClass.getInterfaces()));
        visited = new HashSet<>();
        newInterfaces = new LinkedList<>();
        ImplMethods = new HashSet<>();

        while (!interfaces.isEmpty()){
            for (Class i : interfaces) {
                if (visited.contains(i) && i.getMethods().length != 0) {
                    HashSet<Method> ImplMethodsForCheck = new HashSet<>();
                    ImplMethodsForCheck.addAll(ImplMethods);
                    for (Method instM: i.getDeclaredMethods()){
                        ImplMethodsForCheck.remove(instM);
                    }
                    for(Method instM: i.getDeclaredMethods()){
                        for(Method prevM : ImplMethodsForCheck){
                            if(prevM.getName().equals(instM.getName()) && areArraysEqual(prevM.getParameterTypes(),instM.getParameterTypes())){
                                break;
                            }
                        }
                        throw new OOPInherentAmbiguity(interfaceClass, instM.getDeclaringClass(), instM);
                    }
                }
                newInterfaces.addAll(Arrays.asList(i.getInterfaces()));
                visited.add(i);
                ImplMethods.addAll(Arrays.asList(i.getDeclaredMethods()));
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
    }

    private boolean areArraysEqual(Class<?>[] parameterTypes1, Class<?>[] parameterTypes2) {
        List<Class> parameterTypes2List = Arrays.asList(parameterTypes2);
        for(Class c: parameterTypes1){
            if(!parameterTypes2List.contains(c)) return false;
        }
        return true;
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
        for (Method method : klass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();

            int parameterTypesLength = 0;
            int parametersLength = 0;
            if(parameterTypes!= null) parameterTypesLength = parameterTypes.length;
            if(parameters!= null) parametersLength = parameters.length;

            if (parameterTypesLength != parametersLength) continue;

            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(parameters[i])) {
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

    private int findCIndex(String name){
        int i = name.length()-1;
        while(i>=0 && !(name.charAt(i)+ "").equals(".") ){
            i--;
        }
        return i+1 ;
    }


    private String toI (String name){
        int idx = findCIndex(name);
        StringBuilder klassName = new StringBuilder(name);
        klassName.setCharAt(idx, 'I');
        return klassName.toString();
    }

    private String toC (String name){
        int idx = findCIndex(name);
        StringBuilder klassName = new StringBuilder(name);
        klassName.setCharAt(idx, 'C');
        return klassName.toString();
    }

    List<Method> checkInInterfaceClass(Class in, String methodName, Class[] argsClass)
            throws NoSuchMethodException{
        String inName = in.getName();
        String klassName = toC(inName);
        Method me;
        Class klass;
        try{
            klass = Class.forName(klassName);
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
        HashMap<Integer,LinkedList<Method>> distMap = new HashMap<>();
        for(Method met : matches){
            int dist = distance(cArgs,met.getParameterTypes());
            if(distMap.containsKey(dist)){
                LinkedList<Method> metList = distMap.get(dist);
                metList.add(met);
            }else{
                LinkedList<Method> metList = new LinkedList<>();
                metList.add(met);
                distMap.put(dist,metList);
            }
        }
        min = Collections.min(distMap.keySet());
        LinkedList<Method> metList = distMap.get(min);
        if (metList.size() > 1){
            HashSet<Integer> levels= new HashSet<>() ;
            for(Method met1 :metList){
                int newLevel = findLevel(met1.getDeclaringClass());
                if(levels.contains(newLevel) && newLevel == Collections.min(levels)){
                    for(Method met:metList){
                        String inName = toI(met.getDeclaringClass().getName());
                        Class inClass = null;
                        try{inClass = Class.forName(inName);} catch(Exception e){}
                        Pair<Class<?>,Method> p = new Pair(inClass,met);
                        candidates.add(p);
                    }
                    throw new OOP.Provided.Multiple.OOPCoincidentalAmbiguity(candidates);
                }
                levels.add(newLevel);
            }
        }
        toInvoke = metList.getFirst();
        Object ret = null;
        try{
            ret = toInvoke.invoke(toInvoke.getDeclaringClass().newInstance(),args);
        }catch (Exception e){ /*which exception should be thrown here? */}
        return ret;
    }

    private int findLevel(Class<?> declaringClass) {
        int level = 1;
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(interfaceClass.getInterfaces()));
        List<Class> newInterfaces = new LinkedList<>();
        while (!interfaces.isEmpty()) {
            for (Class in : interfaces) {
                if(toI(in.getName()).equals(toI(declaringClass.getName()))) return level;
                newInterfaces.addAll(Arrays.asList(in.getInterfaces()));
            }
            level ++;
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
        return -1;
    }

    //TODO: add more of your code :


    //TODO: DO NOT CHANGE !!!!!!
    public void removeSourceFile() {
        if (sourceFile.exists()) {
            sourceFile.delete();
        }
    }
}
