package OOP.Solution.Trait;

import OOP.Provided.Trait.OOPBadClass;
import OOP.Provided.Trait.OOPTraitConflict;
import OOP.Provided.Trait.OOPTraitException;
import OOP.Provided.Trait.OOPTraitMissingImpl;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class OOPTraitControl {

    //TODO: DO NOT CHANGE !!!!!!
    private Class<?> traitCollector;
    private File sourceFile;
    private HashMap<String, Object> instances;

    //TODO: DO NOT CHANGE !!!!!!
    public OOPTraitControl(Class<?> traitCollector, File sourceFile) {
        this.traitCollector = traitCollector;
        this.sourceFile = sourceFile;
        instances = new HashMap<>();
        initializeInstMap();
    }

    private void initializeInstMap(){
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(traitCollector.getInterfaces()));
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new LinkedList<>();
        while (!interfaces.isEmpty()){
            for (Class in : interfaces) {
                if(visited.contains(in)) continue;
                addInstanceToMap(in);
                newInterfaces.addAll(Arrays.asList(in.getInterfaces()));
                visited.add(in);
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
    }

    private void addInstanceToMap(Class in) {
        String inName = in.getName();
        Class klass;
        Object inst;
        try{
            klass = Class.forName(toC(inName));
            inst = klass.newInstance();
        } catch(Exception e){ return; }
        instances.put(inName, inst);
    }

    private int findCIndex(String name){
        int i = name.length()-1;
        while(i>=0 && !(name.charAt(i)+ "").equals(".") ){
            i--;
        }
        return i+1 ;
    }

    private String toC (String name){
        int idx = findCIndex(name);
        StringBuilder klassName = new StringBuilder(name);
        klassName.setCharAt(idx, 'C');
        return klassName.toString();
    }

    private boolean isC(String name){
        int idx = findCIndex(name);
        return (name.charAt(idx) == 'C');
    }

    private String toT (String name){
        int idx = findCIndex(name);
        StringBuilder klassName = new StringBuilder(name);
        klassName.setCharAt(idx, 'T');
        return klassName.toString();
    }

    private int classDistance(Class source, Class dest){
        int delta = 0;
        String destName = dest.getName();
        for(Class c = source ; c.getName() != destName ; c = c.getSuperclass()){
            delta++;
        }
        return delta;
    }

    private int distance(Class[] sourceTypes, Class[] destTypes){
        int dist = 0;
        for(int i =0 ; i< destTypes.length ; i++){
            Class currSource = sourceTypes[i];
            Class currDest = destTypes[i];
            if (!currDest.getName().equals(currSource.getName())){
                dist += classDistance(currSource,currDest);
            }
        }
        return dist;
    }

    private boolean checkInClass(Class in, Method me){
        String inName = in.getName();
        Method classMethod;
        Class klass;
        OOPTraitMethod newAnnotation;
        try{
            klass = Class.forName(toC(inName));
        }catch(Exception e){ return false;}
        try{
            classMethod = klass.getMethod(me.getName(),me.getParameterTypes());
        } catch(Exception e){ return false;}
        try {
            newAnnotation = classMethod.getAnnotation(OOPTraitMethod.class);
        } catch(Exception e){ return false;}
        if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)){
            return true;
        }
        return false;
    }

    boolean findRealImpl(Method me){
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(traitCollector.getInterfaces()));
        List<Class> nextLevel = new LinkedList<>();
        Method newImpl = null;
        String methodName = me.getName();
        Class[] paramTypes = me.getParameterTypes();
        while (!interfaces.isEmpty()) {
            try {
                for (Class i : interfaces) {
                    try {
                        newImpl = i.getMethod(methodName, paramTypes);
                    } catch (Exception e){
                        if(checkInClass(i,me)) return true;
                        continue; }
                    OOPTraitMethod newAnnotation = newImpl.getAnnotation(OOPTraitMethod.class);

                    if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)){
                        return true;
                    }
                    nextLevel.addAll(Arrays.asList(i.getInterfaces()));
                }
                interfaces.clear();
                interfaces.addAll(nextLevel);
                nextLevel.clear();
            } catch (Exception e) {}
        }
        return false;
    }

    boolean isAbs(Method me){
        if (!me.isAnnotationPresent(OOPTraitBehaviour.class)) return false;
        OOPTraitMethodModifier val = me.getAnnotation(OOPTraitMethod.class).modifier();
        if (val == OOPTraitMethodModifier.INTER_ABS ) return true;
        return false;
    }

    void checkAllAbs(Class c)throws OOPTraitException{
        Method[] absMethods = Arrays.stream(c.getMethods()).
                filter(me -> isAbs(me)).toArray(Method[]::new);
        for (Method me : absMethods){
            if(!findRealImpl(me)) throw new OOPTraitMissingImpl(me);
        }
    }

    //TODO: fill in here :
    public void validateTraitLayout() throws OOPTraitException {
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(traitCollector.getInterfaces()));
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new LinkedList<>();
        try{ checkAllAbs(traitCollector);} catch(OOPTraitException e){throw e;}
            while (!interfaces.isEmpty()){
            for (Class i : interfaces) {
                try{ checkAllAbs(i);} catch(OOPTraitException e){throw e;}
                if (!i.isAnnotationPresent(OOPTraitBehaviour.class)){

                    throw new OOPBadClass(i);
                }
                for(Method m : i.getDeclaredMethods()){
                    if (!m.isAnnotationPresent(OOPTraitMethod.class)){
                        throw new OOPBadClass(m);
                    }else{
                        OOPTraitMethod anno =  m.getAnnotation(OOPTraitMethod.class);
                        if(anno.modifier() == OOPTraitMethodModifier.INTER_CONFLICT){
                            if (!m.isAnnotationPresent(OOPTraitConflictResolver.class)){
                                throw new OOPTraitConflict(m);
                            }
                        }
                    }
                }
                newInterfaces.addAll(Arrays.asList(i.getInterfaces()));
                visited.add(i);
            }
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
    }

    public List<Method> canInvoke(Class[] parameters, String methodName, Class klass) {
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

    private int findLevel(Class<?> declaringClass) {
        int level = 1;
        Class tDeclaringClass = null;
        try {
            tDeclaringClass = Class.forName(toT(declaringClass.getName()));
        } catch (Exception e){}
        List<Class> interfaces = new LinkedList<Class>(Arrays.asList(traitCollector.getInterfaces()));
        List<Class> newInterfaces = new LinkedList<>();
        while (!interfaces.isEmpty()) {
            for (Class in : interfaces) {
                if(toT(in.getName()).equals(toT(tDeclaringClass.getName()))) return level;
                newInterfaces.addAll(Arrays.asList(in.getInterfaces()));
            }
            level ++;
            interfaces.clear();
            interfaces.addAll(newInterfaces);
            newInterfaces.clear();
        }
        return -1;
    }

    //TODO: fill in here :
    public Object invoke(String methodName, Object[] args) throws OOPTraitException {
        Class<?>[] paramTypes = null;
        if (args != null){
            paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
        }
        HashMap<Integer,LinkedList<Method>> distMap = new HashMap<>();
        int dist;
        int min;
        Method method = null;
        Method toInvoke;
        Class classImpl;
        OOPTraitMethod annotation = null;
        try {
            method = traitCollector.getMethod(methodName, paramTypes);
        } catch (Exception e){}
        if(method != null) {
            annotation = method.getAnnotation(OOPTraitMethod.class);
        }

        if(annotation != null && annotation.modifier().equals(OOPTraitMethodModifier.INTER_CONFLICT)){
            OOPTraitConflictResolver conflictAnnotation = method.getAnnotation(OOPTraitConflictResolver.class);
            try {
                Class<?> classInst = conflictAnnotation.resolve();
                Object instance;
                if(instances.containsKey(classInst.getName())){
                    instance = instances.get(classInst.getName());
                } else {
                    instance = instances.get(toT(classInst.getName()));
                }
                Method newImpl = classInst.getMethod(methodName, paramTypes);
                return newImpl.invoke(instance, args);
            } catch (Exception e) {
                throw new OOPBadClass(method);
            }
        } else {
            List<Class> interfaces = new LinkedList<>();
            interfaces.addAll(Arrays.asList(traitCollector.getInterfaces()));
            List<Class> nextLevel = new LinkedList<>();
            while (!interfaces.isEmpty()) {
                for (Class i : interfaces) {
                    String inName = i.getName();
                    List<Method> matches = canInvoke(paramTypes, methodName, i);
                    for(Method newImpl : matches) {
                        OOPTraitMethod newAnnotation = newImpl.getAnnotation(OOPTraitMethod.class);

                        if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)) {
                            dist = distance(paramTypes, newImpl.getParameterTypes());
                            if (distMap.containsKey(dist)) {
                                LinkedList<Method> metList = distMap.get(dist);
                                metList.add(newImpl);
                            } else {
                                LinkedList<Method> metList = new LinkedList<>();
                                metList.add(newImpl);
                                distMap.put(dist, metList);
                            }
                            continue;
                        }

                        if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_CONFLICT)) {
                            OOPTraitConflictResolver conflictAnnotation = method.getAnnotation(OOPTraitConflictResolver.class);

                            try {
                                newImpl = conflictAnnotation.resolve().getMethod(methodName, paramTypes);
                                Class<?> classInst = conflictAnnotation.resolve();
                                Object instance;
                                if (instances.containsKey(classInst.getName())) {
                                    instance = instances.get(classInst.getName());
                                } else {
                                    instance = instances.get(toT(classInst.getName()));
                                }
                                return newImpl.invoke(instance, args);
                            } catch (Exception e) {
                                throw new OOPBadClass(method);
                            }
                        }
                    }

                    if (isC(i.getName())) continue;
                    nextLevel.addAll(Arrays.asList(i.getInterfaces()));
                    String inName1 = i.getName();
                    Class klass1;
                    try {
                         klass1 = Class.forName(toC(inName1));
                        } catch (Exception e) {
                            continue;
                        }
                    nextLevel.add(klass1);

                }
                interfaces.clear();
                interfaces.addAll(nextLevel);
                nextLevel.clear();
            }
        }

        min = Collections.min(distMap.keySet());
        LinkedList<Method> metList = distMap.get(min);
        if (metList.size() > 1) {
        HashSet<Integer> levels = new HashSet<>();
        for (Method me : metList) {
            int newLevel = findLevel(me.getDeclaringClass());
            if (levels.contains(newLevel)) throw new OOPTraitConflict(me);
            levels.add(newLevel);
        }
        }
        toInvoke = metList.getFirst();
        classImpl = toInvoke.getDeclaringClass();

        if (toInvoke != null){
            Object instance;
            if(instances.containsKey(classImpl.getName())) {
                instance = instances.get(classImpl.getName());
            } else {
                instance = instances.get(toT(classImpl.getName()));
            }
            try {
                return toInvoke.invoke(instance, args);
            } catch (Exception e){}

        } else {
            throw new OOPTraitMissingImpl(method);
        }
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
