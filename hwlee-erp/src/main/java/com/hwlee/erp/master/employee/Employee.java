package com.hwlee.erp.master.employee;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import com.hwlee.erp.master.department.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 직원 마스터 — 부서에 속하며, Phase 6 부터는 로그인 사용자(email)이 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "employee")
@SQLDelete(sql = "UPDATE employee SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Employee extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    public static Employee create(String code, String name, String email,
                                  Department department, LocalDate hireDate) {
        validate(name, email, department, hireDate);
        Employee e = new Employee();
        e.assignCode(code);
        e.name = name;
        e.email = email;
        e.department = department;
        e.hireDate = hireDate;
        return e;
    }

    public void update(String name, String email, Department department, LocalDate hireDate) {
        validate(name, email, department, hireDate);
        this.name = name;
        this.email = email;
        this.department = department;
        this.hireDate = hireDate;
    }

    private static void validate(String name, String email, Department department, LocalDate hireDate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email 은 비어 있을 수 없다.");
        }
        if (department == null) {
            throw new IllegalArgumentException("department 는 null 일 수 없다.");
        }
        if (hireDate == null) {
            throw new IllegalArgumentException("hireDate 는 null 일 수 없다.");
        }
    }
}
