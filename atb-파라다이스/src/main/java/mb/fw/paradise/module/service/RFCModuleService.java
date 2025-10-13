package mb.fw.paradise.module.service;

import org.springframework.stereotype.Service;

import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;

@Service
@ConditionalOnAdaptorType(AdaptorType.RFC)
public class RFCModuleService {

}
