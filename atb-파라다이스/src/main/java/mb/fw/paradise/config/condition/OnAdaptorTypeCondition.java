package mb.fw.paradise.config.condition;

import java.util.Arrays;
import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;

public class OnAdaptorTypeCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnAdaptorType.class.getName());
		if (attrs == null)
			return false;

		AdaptorType[] expectedTypes = (AdaptorType[]) attrs.get("value");
		boolean negate = (boolean) attrs.getOrDefault("negate", false);
		String actualType = context.getEnvironment().getProperty("adaptor.type");

		boolean matched = Arrays.stream(expectedTypes)
				.anyMatch(expected -> expected.name().equalsIgnoreCase(actualType));

		return negate ? !matched : matched;
	}

}
