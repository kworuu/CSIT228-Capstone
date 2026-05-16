

// Replaced jakarta with javax if you downgraded, or keep jakarta if dependencies are resolved.
import com.example.model.EvacuationCenter;
import com.example.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evacuees")
public class Evacuee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name_enc", nullable = false, length = 500)
    private String fullNameEnc;

    @Column(name = "contact_enc", length = 500)
    private String contactEnc;

    // No imports needed for User if it is in com.civicguard.model
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    // No imports needed for EvacuationCenter if it is in com.civicguard.model
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evacuation_center_id", nullable = false)
    private EvacuationCenter evacuationCenter;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Constructors
    public Evacuee() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullNameEnc() { return fullNameEnc; }
    public void setFullNameEnc(String fullNameEnc) { this.fullNameEnc = fullNameEnc; }

    public String getContactEnc() { return contactEnc; }
    // FIXED: Corrected syntax parenthesis below
    public void setContactEnc(String contactEnc) { this.contactEnc = contactEnc; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public EvacuationCenter getEvacuationCenter() { return evacuationCenter; }
    public void setEvacuationCenter(EvacuationCenter evacuationCenter) { this.evacuationCenter = evacuationCenter; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}