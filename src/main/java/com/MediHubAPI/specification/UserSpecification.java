package com.MediHubAPI.specification;

import com.MediHubAPI.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasRole(ERole role) {
        return (root, query, cb) -> cb.equal(
                root.join("roles").get("name"),
                role
        );
    }

    public static Specification<User> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), likePattern),
                    cb.like(cb.lower(root.get("email")), likePattern),
                    cb.like(cb.lower(root.get("firstName")), likePattern),
                    cb.like(cb.lower(root.get("lastName")), likePattern)
            );
        };
    }

//    public static Specification<User> hasSpecialization(String specialization) {
//        return (root, query, cb) ->
//                cb.equal(cb.lower(root.join("specialization").get("name")), specialization.toLowerCase());
//    }
public static Specification<User> hasSpecialization(String specialization) {
    return (root, query, cb) -> {
        Join<Object, Object> join = root.join("specialization", JoinType.LEFT);
        return cb.equal(
                cb.lower(join.get("name")),
                specialization.toLowerCase()
        );
    };
}


}
