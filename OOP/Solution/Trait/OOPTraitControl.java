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
        Object inst = null;
        try{
            klass = Class.forName(toC(inName));
            inst = klass.newInstance();
        }catch(Exception e){}
        instances.put(inName, inst);
    }

    private int findCIndex(String name){
        int i = name.length()-1;
        while(i>=0 && (name.charAt(i)+ "").matches("[0-9]") ){
            i--;
        }
        return i ;
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
                dist+=classDistance(currSource,currDest);
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

    //TODO: fill in here :
    public Object invoke(String methodName, Object[] args) throws OOPTraitException {
        Class<?>[] paramTypes = null;
        if (args != null){
            paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
        }

        int dist;
        int min =- 1;
        Method method;
        Method toInvoke = null;
        Class classImpl = null;
        try {
            method = traitCollector.getMethod(methodName, paramTypes);
        } catch (Exception e){ return null; }
        OOPTraitMethod annotation = method.getAnnotation(OOPTraitMethod.class);

        if(annotation.modifier().equals(OOPTraitMethodModifier.INTER_CONFLICT)){
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
        }

      else {
            List<Class> interfaces = new LinkedList<>();
            interfaces.addAll(Arrays.asList(traitCollector.getInterfaces()));
            List<Class> nextLevel = new LinkedList<>();
            Method newImpl;

            if(annotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)){
                dist = distance(paramTypes, method.getParameterTypes());
                if (dist == min) {
                    throw new OOP.Provided.Trait.OOPTraitConflict(method);
                }
                if (min == -1 || dist < min) {
                    min = dist;
                    toInvoke = method;
                    classImpl = method.getDeclaringClass();
                }
            }

            while (!interfaces.isEmpty()) {
                for (Class i : interfaces) {
                    try {
                        newImpl = i.getMethod(methodName, paramTypes);
                    } catch (Exception e) {
                        continue;
                    }
                    OOPTraitMethod newAnnotation = newImpl.getAnnotation(OOPTraitMethod.class);

                    if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)) {
                        dist = distance(paramTypes, newImpl.getParameterTypes());
                        if (dist == min) {
                            throw new OOP.Provided.Trait.OOPTraitConflict(newImpl);
                        }
                        if (min == -1 || dist < min) {
                            min = dist;
                            toInvoke = newImpl;
                            classImpl = i;
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

                    if (isC(i.getName())) continue;
                    nextLevel.addAll(Arrays.asList(i.getInterfaces()));
                    String inName = i.getName();
                    Class klass = null;
                    try {
                        klass = Class.forName(toC(inName));
                    } catch (Exception e) {
                    }
                    nextLevel.add(klass);
                }
                interfaces.clear();
                interfaces.addAll(nextLevel);
                nextLevel.clear();

            }
        }

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
