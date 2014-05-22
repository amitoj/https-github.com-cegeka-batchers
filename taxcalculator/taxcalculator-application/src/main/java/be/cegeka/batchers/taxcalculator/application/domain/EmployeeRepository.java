package be.cegeka.batchers.taxcalculator.application.domain;


import be.cegeka.batchers.taxcalculator.to.EmployeeTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

@Repository
@Transactional(isolation = Isolation.DEFAULT)
public class EmployeeRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    public void save(Employee employee) {
        entityManager.persist(employee);
    }

    public Employee getBy(Long id) {
        return entityManager.find(Employee.class, id);
    }

    public Long count() {

        CriteriaBuilder criteriaBuilder = entityManagerFactory.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        criteriaQuery.select(
                criteriaBuilder.count(
                        criteriaQuery.from(Employee.class))
        );

        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    public List<Employee> getAll() {
        CriteriaBuilder criteriaBuilder = entityManagerFactory.getCriteriaBuilder();
        CriteriaQuery<Employee> criteriaQuery = criteriaBuilder.createQuery(Employee.class);

        criteriaQuery.select(criteriaQuery.from(Employee.class));

        return entityManager.createQuery(criteriaQuery).getResultList();

    }

    public List<Employee> getFirst20() {
        TypedQuery<Employee> first20 = entityManager.createNamedQuery(Employee.GET_ALL_NAME, Employee.class);
        first20.setMaxResults(20);

        return first20.getResultList();
    }

    public List<EmployeeTo> getFirst20WithTax() {
        TypedQuery<EmployeeTo> first20 = entityManager.createNamedQuery(Employee.GET_EMPLOYEES_TOTAL_TAX_NAME, EmployeeTo.class);
        first20.setMaxResults(20);

        return first20.getResultList();
    }

    public void deleteAll() {
        CriteriaBuilder criteriaBuilder = entityManagerFactory.getCriteriaBuilder();
        CriteriaDelete<Employee> criteriaDelete = criteriaBuilder.createCriteriaDelete(Employee.class);

        criteriaDelete.from(Employee.class);

        entityManager.createQuery(criteriaDelete).executeUpdate();
    }
}
