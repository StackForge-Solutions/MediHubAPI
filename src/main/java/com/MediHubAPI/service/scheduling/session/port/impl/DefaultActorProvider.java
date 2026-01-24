package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.service.scheduling.session.port.ActorProvider;
import org.springframework.stereotype.Component;

@Component
public class DefaultActorProvider implements ActorProvider {
    @Override
    public String currentActor() {
        // TODO: replace by SecurityContext / AuthService / JWT principal
        return "SYSTEM";
    }
}
