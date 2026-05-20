package com.hwlee.erp.master.employee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByCode(String code);

    boolean existsByEmail(String email);
}
