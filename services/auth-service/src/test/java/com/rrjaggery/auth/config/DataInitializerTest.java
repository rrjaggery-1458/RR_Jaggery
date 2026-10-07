package com.rrjaggery.auth.config;

import com.rrjaggery.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void doesNotCreatePortalAccountsWhenRepositoryIsEmpty() {
        when(userRepository.findAll()).thenReturn(List.of());

        dataInitializer.run();

        verify(userRepository, never()).save(any());
    }
}
