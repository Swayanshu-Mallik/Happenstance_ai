package com.happenstance.relationship;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("relationships")
@CompoundIndex(name="relationship_pair_unique",def="{'sourceUserId':1,'targetUserId':1}",unique=true)
public class Relationship {
 @Id private String id;
 private String sourceUserId;
 private String targetUserId;
 private String type="CONNECTION";
 private String status="PENDING";
 private String visibility="PRIVATE";
 private String provenance="USER_DECLARED";
 private Instant createdAt=Instant.now();
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getSourceUserId(){return sourceUserId;} public void setSourceUserId(String v){sourceUserId=v;}
 public String getTargetUserId(){return targetUserId;} public void setTargetUserId(String v){targetUserId=v;}
 public String getType(){return type;} public void setType(String v){type=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getVisibility(){return visibility;} public void setVisibility(String v){visibility=v;}
 public String getProvenance(){return provenance;} public void setProvenance(String v){provenance=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
