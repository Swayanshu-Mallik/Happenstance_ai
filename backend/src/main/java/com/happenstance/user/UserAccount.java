package com.happenstance.user;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("users")
public class UserAccount {
 @Id private String id;
 @Indexed(unique=true) private String email;
 private String passwordHash;
 private String displayName;
 private String role="USER";
 private Instant createdAt=Instant.now();
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
 public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
 public String getRole(){return role;} public void setRole(String v){role=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
