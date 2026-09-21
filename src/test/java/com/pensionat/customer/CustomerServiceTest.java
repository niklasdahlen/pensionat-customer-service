package com.pensionat.customer;

import com.pensionat.customer.client.BookingClient;
import com.pensionat.customer.dto.CreateCustomerRequest;
import com.pensionat.customer.exception.BadRequestException;
import com.pensionat.customer.exception.ConflictException;
import com.pensionat.customer.exception.NotFoundException;
import com.pensionat.customer.model.CustomerEntity;
import com.pensionat.customer.service.CustomerService;
import com.pensionat.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private PasswordEncoder passwordEncoder;
    private BookingClient bookingClient;
    private CustomerService customerService;

    @BeforeEach
    void setup() {
        customerRepository = mock(CustomerRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        bookingClient = mock(BookingClient.class);
        customerService = new CustomerService(customerRepository, passwordEncoder, bookingClient);
    }
    @Test
    void createCustomer_savesCustomerAndHashesPassword() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Anna", "Andersson", "anna@test.com", "plaintext-password", "0701234567"
        );

        when(passwordEncoder.encode("plaintext-password")).thenReturn("hashed-password");
        when(customerRepository.save(any(CustomerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerEntity saved = customerService.createCustomer(request);

        assertEquals("Anna", saved.getFirstName());
        assertEquals("hashed-password", saved.getHashedPassword());
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @Test
    void deleteCustomer_throwsNotFound_whenCustomerDoesNotExist() {
        when(customerRepository.existsById(999L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> customerService.deleteCustomer(999L));

        verify(customerRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCustomer_throwsConflict_whenCustomerHasActiveBookings() {
        when(customerRepository.existsById(5L)).thenReturn(true);
        when(bookingClient.customerHasActiveBookings(5L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> customerService.deleteCustomer(5L));

        verify(customerRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCustomer_succeeds_whenNoActiveBookings() {
        when(customerRepository.existsById(5L)).thenReturn(true);
        when(bookingClient.customerHasActiveBookings(5L)).thenReturn(false);

        customerService.deleteCustomer(5L);

        verify(customerRepository).deleteById(5L);
    }

}
