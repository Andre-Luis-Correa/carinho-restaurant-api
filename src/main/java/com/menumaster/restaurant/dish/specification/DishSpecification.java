//package com.menumaster.restaurant.dish.specification;
//
//import com.menumaster.restaurant.dish.domain.model.Dish;
//import com.menumaster.restaurant.utils.SearchCriteria;
//import jakarta.persistence.criteria.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.Objects;
//
//@RequiredArgsConstructor
//public class DishSpecification implements Specification<Dish> {
//
//    private final SearchCriteria searchCriteria;
//
//    @Override
//    public Predicate toPredicate(Root<Dish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//        if (Objects.nonNull(searchCriteria.getValue())) {
//            switch (searchCriteria.getOperation()) {
//                case ":":
//                    if (root.get(searchCriteria.getKey()).getJavaType() == String.class) {
//                        // Busca por string com LIKE (ignora maiúsculas/minúsculas)
//                        return criteriaBuilder.like(
//                                criteriaBuilder.lower(root.get(searchCriteria.getKey())),
//                                "%" + searchCriteria.getValue().toString().toLowerCase() + "%"
//                        );
//                    } else {
//                        // Busca por igualdade
//                        return criteriaBuilder.equal(root.get(searchCriteria.getKey()), searchCriteria.getValue());
//                    }
//
//                case ">":
//                    if (searchCriteria.getValue() instanceof Number) {
//                        // Comparação maior ou igual para tipos numéricos
//                        return criteriaBuilder.greaterThanOrEqualTo(
//                                root.get(searchCriteria.getKey()).as(Number.class), (Number) searchCriteria.getValue());
//                    }
//                    break;
//
//                case "<":
//                    if (searchCriteria.getValue() instanceof Number) {
//                        // Comparação menor ou igual para tipos numéricos
//                        return criteriaBuilder.lessThanOrEqualTo(
//                                root.get(searchCriteria.getKey()).as(Number.class), (Number) searchCriteria.getValue());
//                    }
//                    break;
//            }
//        }
//
//        // Retorna sempre verdadeiro se não houver valor para o critério
//        return criteriaBuilder.conjunction();
//    }
//}