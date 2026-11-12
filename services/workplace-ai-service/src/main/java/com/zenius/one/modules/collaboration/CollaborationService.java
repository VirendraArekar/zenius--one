package com.zenius.one.modules.collaboration;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import com.zenius.one.platform.notifications.NotificationService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationService.class);

    private final ColMailMessageRepository mailRepository;
    private final ColChannelRepository channelRepository;
    private final ColMessageRepository messageRepository;
    private final ColMeetingRepository meetingRepository;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;
    private final boolean smtpEnabled;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpFrom;

    public CollaborationService(
            ColMailMessageRepository mailRepository,
            ColChannelRepository channelRepository,
            ColMessageRepository messageRepository,
            ColMeetingRepository meetingRepository,
            NotificationService notificationService,
            JdbcTemplate jdbcTemplate,
            @Value("${zenius.mail.smtp.enabled:false}") boolean smtpEnabled,
            @Value("${zenius.mail.smtp.host:localhost}") String smtpHost,
            @Value("${zenius.mail.smtp.port:25}") int smtpPort,
            @Value("${zenius.mail.from:noreply@zeniusone.local}") String smtpFrom
    ) {
        this.mailRepository = mailRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.meetingRepository = meetingRepository;
        this.notificationService = notificationService;
        this.jdbcTemplate = jdbcTemplate;
        this.smtpEnabled = smtpEnabled;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpFrom = smtpFrom;
    }

    @Transactional(readOnly = true)
    public List<MailDto> listMail(String folder) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<ColMailMessageEntity> items;
        if (folder == null || folder.isBlank() || "ALL".equalsIgnoreCase(folder)) {
            items = mailRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        } else if ("STARRED".equalsIgnoreCase(folder)) {
            items = mailRepository.findByOrganizationIdAndStarredTrueOrderByCreatedAtDesc(orgId);
        } else {
            items = mailRepository.findByOrganizationIdAndFolderOrderByCreatedAtDesc(orgId, folder.trim().toUpperCase());
        }
        return items.stream().map(this::toMailDto).toList();
    }

    @Transactional
    public MailDto sendMail(ZeniusUserPrincipal principal, SendMailRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String recipient = req.recipientEmail().trim().toLowerCase();

        ColMailMessageEntity sent = new ColMailMessageEntity();
        sent.setOrganizationId(orgId);
        sent.setFolder("SENT");
        sent.setSenderEmail(principal.getEmail());
        sent.setRecipientEmail(recipient);
        sent.setSubject(req.subject().trim());
        sent.setBody(req.body().trim());
        sent.setLabels(req.labels());
        sent.setRead(true);
        sent = mailRepository.save(sent);

        ColMailMessageEntity inbox = new ColMailMessageEntity();
        inbox.setOrganizationId(orgId);
        inbox.setFolder("INBOX");
        inbox.setSenderEmail(principal.getEmail());
        inbox.setRecipientEmail(recipient);
        inbox.setSubject(sent.getSubject());
        inbox.setBody(sent.getBody());
        inbox.setLabels(sent.getLabels());
        inbox.setRead(false);
        mailRepository.save(inbox);

        UUID notifyUserId = jdbcTemplate.query(
                        "SELECT id FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1",
                        (rs, rowNum) -> UUID.fromString(rs.getString(1)),
                        recipient)
                .stream()
                .findFirst()
                .orElse(principal.getUserId());

        notificationService.send(
                orgId,
                notifyUserId,
                "New Email: " + sent.getSubject(),
                "From " + principal.getEmail() + " to " + recipient,
                "MAIL",
                "/collaboration"
        );

        dispatchOptionalSmtp(principal.getEmail(), recipient, sent.getSubject(), sent.getBody());
        return toMailDto(sent);
    }

    @Transactional
    public MailDto setMailRead(UUID mailId, boolean read) {
        UUID orgId = TenantContext.requireOrganizationId();
        ColMailMessageEntity entity = mailRepository.findByIdAndOrganizationId(mailId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Mail message not found"));
        entity.setRead(read);
        return toMailDto(mailRepository.save(entity));
    }

    @Transactional
    public MailDto toggleStar(UUID mailId) {
        UUID orgId = TenantContext.requireOrganizationId();
        ColMailMessageEntity entity = mailRepository.findByIdAndOrganizationId(mailId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Mail message not found"));
        entity.setStarred(!entity.isStarred());
        return toMailDto(mailRepository.save(entity));
    }

    @Transactional
    public MailDto replyMail(ZeniusUserPrincipal principal, UUID originalId, String body) {
        UUID orgId = TenantContext.requireOrganizationId();
        ColMailMessageEntity original = mailRepository.findByIdAndOrganizationId(originalId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Original email not found"));
        String replyTo = original.getSenderEmail().equalsIgnoreCase(principal.getEmail())
                ? original.getRecipientEmail()
                : original.getSenderEmail();
        String subject = original.getSubject().startsWith("Re:") ? original.getSubject() : "Re: " + original.getSubject();
        return sendMail(principal, new SendMailRequest(replyTo, subject, body, original.getLabels()));
    }

    private void dispatchOptionalSmtp(String replyTo, String to, String subject, String body) {
        if (!smtpEnabled || smtpHost == null || smtpHost.isBlank()) {
            return;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(smtpHost, smtpPort), 3000);
            socket.setSoTimeout(3000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reader.readLine();
            for (String cmd : List.of(
                    "HELO zeniusone.local",
                    "MAIL FROM:<" + smtpFrom + ">",
                    "RCPT TO:<" + to + ">",
                    "DATA"
            )) {
                writer.write(cmd + "\r\n");
                writer.flush();
                reader.readLine();
            }
            writer.write("From: " + smtpFrom + "\r\nReply-To: " + replyTo + "\r\nTo: " + to + "\r\nSubject: " + subject + "\r\n\r\n" + body + "\r\n.\r\n");
            writer.flush();
            reader.readLine();
            writer.write("QUIT\r\n");
            writer.flush();
        } catch (Exception ex) {
            log.warn("Optional SMTP relay skipped/failed for {}: {}", to, ex.getMessage());
        }
    }

    private MailDto toMailDto(ColMailMessageEntity m) {
        return new MailDto(m.getId(), m.getFolder(), m.getSenderEmail(), m.getRecipientEmail(), m.getSubject(), m.getBody(), m.getLabels(), m.isRead(), m.isStarred(), m.getCreatedAt());
    }

    @Transactional
    public List<ChannelDto> listChannels() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<ColChannelEntity> channels = channelRepository.findByOrganizationIdOrderByNameAsc(orgId);
        if (channels.isEmpty()) {
            ColChannelEntity general = new ColChannelEntity();
            general.setOrganizationId(orgId);
            general.setName("general");
            general.setDescription("Organization-wide announcements and team chat");
            general.setPrivateChannel(false);
            channels = List.of(channelRepository.save(general));
        }
        return channels.stream()
                .map(c -> new ChannelDto(c.getId(), c.getName(), c.getDescription(), c.isPrivateChannel()))
                .toList();
    }

    @Transactional
    public ChannelDto createChannel(CreateChannelRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String name = req.name().trim().toLowerCase().replaceAll("\\s+", "-");
        if (channelRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, name)) {
            throw new IllegalStateException("Channel already exists: #" + name);
        }
        ColChannelEntity c = new ColChannelEntity();
        c.setOrganizationId(orgId);
        c.setName(name);
        c.setDescription(req.description());
        c.setPrivateChannel(req.privateChannel());
        c = channelRepository.save(c);
        return new ChannelDto(c.getId(), c.getName(), c.getDescription(), c.isPrivateChannel());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> listMessages(UUID channelId) {
        UUID orgId = TenantContext.requireOrganizationId();
        channelRepository.findByIdAndOrganizationId(channelId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found in current organization"));
        return messageRepository.findByOrganizationIdAndChannelIdOrderByCreatedAtAsc(orgId, channelId).stream()
                .map(m -> new MessageDto(m.getId(), m.getChannelId(), m.getSenderName(), m.getBody(), m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public MessageDto postMessage(ZeniusUserPrincipal principal, UUID channelId, String body) {
        UUID orgId = TenantContext.requireOrganizationId();
        channelRepository.findByIdAndOrganizationId(channelId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found in current organization"));
        ColMessageEntity m = new ColMessageEntity();
        m.setOrganizationId(orgId);
        m.setChannelId(channelId);
        m.setSenderName(principal.getFullName());
        m.setBody(body.trim());
        m = messageRepository.save(m);
        return new MessageDto(m.getId(), m.getChannelId(), m.getSenderName(), m.getBody(), m.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<MeetingDto> listMeetings() {
        UUID orgId = TenantContext.requireOrganizationId();
        return meetingRepository.findByOrganizationIdOrderByScheduledAtAsc(orgId).stream()
                .map(m -> new MeetingDto(m.getId(), m.getTitle(), m.getOrganizerName(), m.getScheduledAt(), m.getDurationMinutes(), m.getMeetingLink()))
                .toList();
    }

    @Transactional
    public MeetingDto scheduleMeeting(ZeniusUserPrincipal principal, CreateMeetingRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ColMeetingEntity m = new ColMeetingEntity();
        m.setOrganizationId(orgId);
        m.setTitle(req.title().trim());
        m.setOrganizerName(principal.getFullName());
        m.setScheduledAt(req.scheduledAt() != null ? req.scheduledAt() : Instant.now().plus(1, ChronoUnit.HOURS));
        m.setDurationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 30);
        m.setMeetingLink("https://meet.zeniusone.local/" + UUID.randomUUID().toString().substring(0, 8));
        m = meetingRepository.save(m);
        return new MeetingDto(m.getId(), m.getTitle(), m.getOrganizerName(), m.getScheduledAt(), m.getDurationMinutes(), m.getMeetingLink());
    }

    public record MailDto(UUID id, String folder, String senderEmail, String recipientEmail, String subject, String body, String labels, boolean read, boolean starred, Instant createdAt) {
    }

    public record SendMailRequest(String recipientEmail, String subject, String body, String labels) {
    }

    public record ChannelDto(UUID id, String name, String description, boolean privateChannel) {
    }

    public record CreateChannelRequest(String name, String description, boolean privateChannel) {
    }

    public record MessageDto(UUID id, UUID channelId, String senderName, String body, Instant createdAt) {
    }

    public record CreateMeetingRequest(String title, Instant scheduledAt, Integer durationMinutes) {
    }

    public record MeetingDto(UUID id, String title, String organizerName, Instant scheduledAt, Integer durationMinutes, String meetingLink) {
    }
}

