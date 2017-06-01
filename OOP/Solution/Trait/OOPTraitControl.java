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

    private String toC (String name){
        StringBuilder klassName = new StringBuilder(name);
        klassName.setCharAt(name.length()-2, 'C');
        return klassName.toString();
    }

    private String toT (String name){
        StringBuilder klassName = new StringBuilder(name);
        klassName.setCharAt(name.length()-2, 'T');
        return klassName.toString();
    }

    boolean checkInClass(Class in, Method me){
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
        Method method = null;
        try {
            method = traitCollector.getMethod(methodName, paramTypes);
        } catch (Exception e){ return null; }
        OOPTraitMethod annotation = method.getAnnotation(OOPTraitMethod.class);

        if(annotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)){
            try {
                Object inst;
                String key = method.getDeclaringClass().getName();
                if(instances.containsKey(key)) {
                    inst = instances.get(key);
                } else {
                    inst = instances.get(toT(key));
                }
                return method.invoke(inst, args);
            } catch (Exception e){}

        }

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
            }catch (Exception e) {
                throw new OOPBadClass(method);
            }
            }

        if(annotation.modifier().equals(OOPTraitMethodModifier.INTER_ABS) ||
                annotation.modifier().equals(OOPTraitMethodModifier.INTER_MISSING_IMPL)){

            List<Class> interfaces = new LinkedList<>();
            interfaces.addAll(Arrays.asList(traitCollector.getInterfaces()));
            List<Class> nextLevel = new LinkedList<>();
            boolean find_imp = false;
            Method realImpl = null;
            Method newImpl;
            Class classImpl = null;

            while (!interfaces.isEmpty()) {
                try {
                    for (Class i : interfaces) {
                        try {
                            newImpl = i.getMethod(methodName, paramTypes);
                        } catch (Exception e){ continue; }
                        OOPTraitMethod newAnnotation = newImpl.getAnnotation(OOPTraitMethod.class);

                        if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)) {
                            if (find_imp) {
                                throw new OOPTraitConflict(method);
                            }
                            find_imp = true;
                            realImpl = newImpl;
                            classImpl = i ;
                            continue;
                        }

                        if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_CONFLICT)) {
                            OOPTraitConflictResolver conflictAnnotation = method.getAnnotation(OOPTraitConflictResolver.class);
                            newImpl = conflictAnnotation.resolve().getMethod(methodName, paramTypes);
                            if (find_imp) {
                                throw new OOPTraitConflict(method);
                            }
                            find_imp = true;
                            realImpl = newImpl;
                            classImpl = i;
                            continue;
                        }
                        nextLevel.addAll(Arrays.asList(i.getInterfaces()));
                        String inName = i.getName();
                        Class klass = Class.forName(toC(inName));
                        nextLevel.add(klass);
                    }
                    if(find_imp){
                        Object instance;
                        if(instances.containsKey(classImpl.getName())) {
                            instance = instances.get(classImpl.getName());
                        } else {
                            instance = instances.get(toT(classImpl.getName()));
                        }

                        return realImpl.invoke(instance, args);
                    }
                    interfaces.clear();
                    interfaces.addAll(nextLevel);
                    nextLevel.clear();
                } catch (Exception e) { return null; }
            }
            if (realImpl == null){
                throw new OOPTraitMissingImpl(method);
            }
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
