package mb.fw.paradise.config.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import mb.fw.paradise.config.condition.OnAdaptorTypeCondition;
import mb.fw.paradise.constants.AdaptorType;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnAdaptorTypeCondition.class)
public @interface ConditionalOnAdaptorType {
	AdaptorType value();
}
