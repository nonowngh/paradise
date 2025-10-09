package mb.fw.paradise.config.condition;

import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;

public class OnAdaptorTypeCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String configuredType = context.getEnvironment().getProperty("adaptor.type");

		if (metadata.isAnnotated(ConditionalOnAdaptorType.class.getName())) {
			Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnAdaptorType.class.getName());
			AdaptorType requiredType = (AdaptorType) attrs.get("value");

			return requiredType.name().equalsIgnoreCase(configuredType);
		}
		return false;
	}

}
