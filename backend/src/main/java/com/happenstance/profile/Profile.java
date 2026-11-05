package com.happenstance.profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Document("profiles")
public class Profile {
 @Id private String id;
 @Indexed(unique=true) private String userId;
 private String headline="";
 private String bio="";
 private String location="";
 private String industry="";
 private List<String> skills=new ArrayList<>();
 private List<String> interests=new ArrayList<>();
 private String visibility="PUBLIC";
 private Instant updatedAt=Instant.now();
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 public String getHeadline(){return headline;} public void setHeadline(String v){headline=v;}
 public String getBio(){return bio;} public void setBio(String v){bio=v;}
 public String getLocation(){return location;} public void setLocation(String v){location=v;}
 public String getIndustry(){return industry;} public void setIndustry(String v){industry=v;}
 public List<String> getSkills(){return skills;} public void setSkills(List<String> v){skills=v;}
 public List<String> getInterests(){return interests;} public void setInterests(List<String> v){interests=v;}
 public String getVisibility(){return visibility;} public void setVisibility(String v){visibility=v;}
 public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
