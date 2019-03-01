package com.example.quartzdemo.Repository;

//import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quartzdemo.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.sound.midi.Track;

@Repository
public interface UserRepository extends CrudRepository<User, String> {
    @Query(value = "SELECT * FROM users WHERE username=?1", nativeQuery = true)
    public User getUserDetails(String username);

}