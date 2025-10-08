package com.MediHubAPI.repository;
// package com.MediHubAPI.repository;

import com.MediHubAPI.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface SymptomCategoryRepository extends JpaRepository<SymptomCategory, Long> {
    Optional<SymptomCategory> findByNameIgnoreCase(String name);
}


