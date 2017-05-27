package OOP.Solution.Trait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OOPTraitMethod {
    OOPTraitMethodModifier modifier() default OOPTraitMethodModifier.INTER_ABS;
}