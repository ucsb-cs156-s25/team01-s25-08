package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.UCSBDate;
import edu.ucsb.cs156.example.entities.UCSBDiningCommonsMenuItem;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The UCSBDateRepository is a repository for UCSBDate entities.
 */
import edu.ucsb.cs156.example.entities.UCSBDiningCommonsMenuItem;

@Repository
public interface UCSBDiningCommonsMenuItemRepository extends CrudRepository<UCSBDiningCommonsMenuItem, Long> {
  
}