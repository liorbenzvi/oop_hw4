package OOP.Solution.Trait;

import OOP.Provided.Trait.OOPBadClass;
import OOP.Provided.Trait.OOPTraitConflict;
import OOP.Provided.Trait.OOPTraitException;
import OOP.Provided.Trait.OOPTraitMissingImpl;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class OOPTraitControl {

    //TODO: DO NOT CHANGE !!!!!!
    private Class<?> traitCollector;
    private File sourceFile;

    //TODO: DO NOT CHANGE !!!!!!
    public OOPTraitControl(Class<?> traitCollector, File sourceFile) {
        this.traitCollector = traitCollector;
        this.sourceFile = sourceFile;
    }

    //TODO: fill in here :
    public void validateTraitLayout() throws OOPTraitException {


        List<Class> interfaces = Arrays.asList(traitCollector.getInterfaces());
        HashSet<Class> visited = new HashSet<>();
        List<Class> newInterfaces = new ArrayList<>();

        while (!interfaces.isEmpty()){
            for (Class i : interfaces) {
                if (!Arrays.asList(i.getAnnotations()).contains(OOPTraitBehaviour.class)){
                    throw new OOPBadClass(i);
                }
                for(Method m : i.getDeclaredMethods()){
                    List<?> annotations = Arrays.asList(m.getAnnotations());
                    if (!annotations.contains(OOPTraitMethod.class)){
                        throw new OOPBadClass(m);
                    }else{
                        OOPTraitMethod anno =  m.getAnnotation(OOPTraitMethod.class);
                        if(anno.modifier() == OOPTraitMethodModifier.INTER_CONFLICT){
                            if (!annotations.contains(OOPTraitConflictResolver.class)){
                                throw new OOPBadClass(m);
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
        Object obj = null; //TODO - how to find obj?
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        Method method = null;
        try {
            method = traitCollector.getMethod(methodName, paramTypes);
        } catch (Exception e){ return null; }

        OOPTraitMethod annotation = method.getAnnotation(OOPTraitMethod.class);
        if (annotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)){
            try {
                return method.invoke(obj, args);
            } catch (Exception e){ return null; }
        }

        if(annotation.modifier().equals(OOPTraitMethodModifier.INTER_CONFLICT)){
            OOPTraitConflictResolver conflictAnnotation = method.getAnnotation(OOPTraitConflictResolver.class);
            try {
                Method newImpl = conflictAnnotation.resolve().getMethod(methodName, paramTypes);
                return newImpl.invoke(obj, args);
            }catch (Exception e) {
                throw new OOPBadClass(method);
            }
            }

        if(annotation.modifier().equals(OOPTraitMethodModifier.INTER_ABS)){

            List<Class> interfaces = Arrays.asList(traitCollector.getInterfaces());
            List<Class> nextLevel = new ArrayList<>();
            boolean find_imp = false;
            Method realImpl = null;
            Method newImpl = null;

            while (!interfaces.isEmpty()) {
                try {
                    for (Class i : interfaces) {
                        try {
                            newImpl = i.getMethod(methodName, paramTypes);
                        } catch (Exception e){ continue; }
                        OOPTraitMethod newAnnotation = method.getAnnotation(OOPTraitMethod.class);

                        if (newAnnotation.modifier().equals(OOPTraitMethodModifier.INTER_IMPL)) {
                            if (find_imp) {
                                throw new OOPTraitConflict(method);
                            }
                            find_imp = true;
                            realImpl = newImpl;
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
                            continue;
                        }
                        nextLevel.addAll(Arrays.asList(i.getInterfaces()));
                    }
                    if(find_imp){
                        return realImpl.invoke(obj, args);
                    }
                    interfaces.clear();
                    interfaces.addAll(nextLevel);
                    nextLevel.clear();
                } catch (Exception e) {
                }
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
