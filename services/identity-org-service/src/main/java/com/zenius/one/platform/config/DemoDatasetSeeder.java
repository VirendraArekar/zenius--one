package com.zenius.one.platform.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates realistic, deterministic, per-module idempotent enterprise dummy data across
 * every Zenius One module & sidebar item (~1,500+ records per organization).
 */
@Service
public class DemoDatasetSeeder {

    private static final String[] INDUSTRIES = {"Software", "FinTech", "Healthcare", "Manufacturing", "Retail", "Logistics"};
    private static final String[] CITIES = {"New York", "San Francisco", "Bengaluru", "London", "Singapore", "Austin"};
    private static final String[] COUNTRIES = {"USA", "USA", "India", "UK", "Singapore", "USA"};
    private static final String[] FIRST_NAMES = {"Aarav", "Sophia", "Liam", "Priya", "Noah", "Ananya", "Ethan", "Maya", "Lucas", "Zara"};
    private static final String[] LAST_NAMES = {"Mehta", "Patel", "Chen", "Kapoor", "Miller", "Sharma", "Kim", "Brooks", "Nair", "Gomez"};
    private static final String[] LEAD_SOURCES = {"WEBSITE", "REFERRAL", "LINKEDIN", "EVENT", "PARTNER"};
    private static final String[] LEAD_STATUSES = {"NEW", "CONTACTED", "QUALIFIED"};
    private static final String[] CHANNELS = {"WEB", "EMAIL", "CHAT", "PHONE"};
    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH", "URGENT"};
    private static final String[] TICKET_STATUSES = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"};
    private static final String[] DEPARTMENTS = {"Engineering", "Sales", "Finance", "Marketing", "People Ops", "Customer Success"};
    private static final String[] TASK_STATUSES = {"TODO", "IN_PROGRESS", "REVIEW", "DONE"};
    private static final String[] ASSET_TYPES = {"LAPTOP", "SERVER", "FIREWALL", "WORKSTATION"};

    private final JdbcTemplate jdbcTemplate;

    public DemoDatasetSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public SeedResult seedOrganization(UUID orgId, UUID ownerUserId) {
        String tag = orgId.toString().substring(0, 6);
        Map<String, Integer> counts = new LinkedHashMap<>();
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        LocalDate today = LocalDate.now();

        // 1. CRM Accounts (100)
        if (countActive("crm_accounts", orgId) == 0) {
            List<Object[]> accountRows = new ArrayList<>(100);
            for (int i = 1; i <= 100; i++) {
                accountRows.add(new Object[]{
                        UUID.randomUUID(), orgId, ownerUserId,
                        String.format("Enterprise Client %03d", i),
                        INDUSTRIES[i % INDUSTRIES.length],
                        "https://client" + i + ".example.com",
                        String.format("+1-555-01%02d", i % 100),
                        BigDecimal.valueOf(250_000L + (i * 17_500L)),
                        50 + (i * 15),
                        i % 3 == 0 ? "CUSTOMER" : i % 2 == 0 ? "OPPORTUNITY" : "PROSPECT",
                        CITIES[i % CITIES.length],
                        COUNTRIES[i % COUNTRIES.length],
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_accounts (id, organization_id, owner_user_id, name, industry, website, phone, annual_revenue, employee_count, lifecycle_stage, billing_city, billing_country, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    accountRows);
            counts.put("crmAccounts", accountRows.size());
        }
        List<UUID> accountIds = queryIds("SELECT id FROM crm_accounts WHERE organization_id = ? AND deleted_at IS NULL", orgId);

        // 2. CRM Contacts (150)
        if (countActive("crm_contacts", orgId) == 0 && !accountIds.isEmpty()) {
            List<Object[]> contactRows = new ArrayList<>(150);
            for (int i = 1; i <= 150; i++) {
                contactRows.add(new Object[]{
                        UUID.randomUUID(), orgId, accountIds.get((i - 1) % accountIds.size()), ownerUserId,
                        FIRST_NAMES[i % FIRST_NAMES.length],
                        LAST_NAMES[(i * 3) % LAST_NAMES.length],
                        String.format("contact.%s.%03d@client.example.com", tag, i),
                        String.format("+1-555-02%02d", i % 100),
                        i % 4 == 0 ? "VP Engineering" : i % 3 == 0 ? "Head of Finance" : "Operations Director",
                        DEPARTMENTS[i % DEPARTMENTS.length],
                        i % 2 == 0 ? "ENTERPRISE" : "MID_MARKET",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_contacts (id, organization_id, account_id, owner_user_id, first_name, last_name, email, phone, job_title, department, segment, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    contactRows);
            counts.put("crmContacts", contactRows.size());
        }
        List<UUID> contactIds = queryIds("SELECT id FROM crm_contacts WHERE organization_id = ? AND deleted_at IS NULL", orgId);

        // 3. CRM Leads (200)
        if (countActive("crm_leads", orgId) == 0) {
            List<Object[]> leadRows = new ArrayList<>(200);
            for (int i = 1; i <= 200; i++) {
                leadRows.add(new Object[]{
                        UUID.randomUUID(), orgId, ownerUserId,
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[(i * 7) % LAST_NAMES.length] + " #" + i,
                        String.format("lead.%s.%03d@prospect.example.com", tag, i),
                        String.format("+1-555-03%02d", i % 100),
                        "Prospect Ventures " + i,
                        "Director of IT",
                        LEAD_SOURCES[i % LEAD_SOURCES.length],
                        LEAD_STATUSES[i % LEAD_STATUSES.length],
                        25 + ((i * 7) % 75),
                        BigDecimal.valueOf(8_000L + (i * 450L)),
                        "Seeded demo prospect record #" + i,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_leads (id, organization_id, owner_user_id, full_name, email, phone, company_name, job_title, source, status, lead_score, estimated_value, notes, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    leadRows);
            counts.put("crmLeads", leadRows.size());
        }

        // 4. CRM Products (50)
        if (countTable("crm_products", orgId) == 0) {
            List<Object[]> productRows = new ArrayList<>(50);
            for (int i = 1; i <= 50; i++) {
                productRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("ZNS-%s-%03d", tag.toUpperCase(), i),
                        "Zenius Cloud Module Pack #" + i,
                        "Enterprise SaaS subscription SKU #" + i,
                        i % 2 == 0 ? "SAAS_LICENSE" : "PROFESSIONAL_SERVICES",
                        BigDecimal.valueOf(199 + (i * 50)),
                        new BigDecimal("18.00"),
                        true,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_products (id, organization_id, sku, name, description, category, unit_price, tax_rate, is_active, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    productRows);
            counts.put("crmProducts", productRows.size());
        }
        List<UUID> productIds = queryIds("SELECT id FROM crm_products WHERE organization_id = ?", orgId);

        // 5. Sales Pipelines, Stages, Deals, Quotes, Orders & Activities
        if (countTable("crm_pipelines", orgId) == 0) {
            UUID pipelineId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO crm_pipelines (id, organization_id, name, is_default, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    pipelineId, orgId, "Enterprise Sales Pipeline", true, 0L, ts, ts, "seeder", "seeder");
            String[][] stages = {
                    {"Prospecting", "20", "1", "false", "false"},
                    {"Discovery", "40", "2", "false", "false"},
                    {"Proposal", "60", "3", "false", "false"},
                    {"Negotiation", "80", "4", "false", "false"},
                    {"Closed Won", "100", "5", "true", "true"}
            };
            List<Object[]> stageRows = new ArrayList<>();
            for (String[] s : stages) {
                stageRows.add(new Object[]{
                        UUID.randomUUID(), orgId, pipelineId, s[0],
                        Integer.parseInt(s[1]), Integer.parseInt(s[2]),
                        Boolean.parseBoolean(s[3]), Boolean.parseBoolean(s[4]),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_stages (id, organization_id, pipeline_id, name, probability, stage_order, is_closed, is_won, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    stageRows);
            counts.put("crmPipelines", 1);
            counts.put("crmStages", stageRows.size());
        }
        List<UUID> pipelineIds = queryIds("SELECT id FROM crm_pipelines WHERE organization_id = ?", orgId);
        List<UUID> stageIds = queryIds("SELECT id FROM crm_stages WHERE organization_id = ? ORDER BY stage_order ASC", orgId);

        if (countActive("crm_deals", orgId) == 0 && !pipelineIds.isEmpty() && !stageIds.isEmpty()) {
            UUID pipelineId = pipelineIds.get(0);
            List<Object[]> dealRows = new ArrayList<>(25);
            for (int i = 1; i <= 25; i++) {
                UUID stageId = stageIds.get((i - 1) % stageIds.size());
                int prob = switch ((i - 1) % 5) {
                    case 0 -> 20;
                    case 1 -> 40;
                    case 2 -> 60;
                    case 3 -> 80;
                    default -> 100;
                };
                BigDecimal amount = BigDecimal.valueOf(24_000L + (i * 4_500L));
                BigDecimal expectedRev = amount.multiply(BigDecimal.valueOf(prob)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                dealRows.add(new Object[]{
                        UUID.randomUUID(), orgId, pipelineId, stageId,
                        accountIds.isEmpty() ? null : accountIds.get((i - 1) % accountIds.size()),
                        contactIds.isEmpty() ? null : contactIds.get((i - 1) % contactIds.size()),
                        ownerUserId,
                        "Zenius Suite Rollout — Deal #" + i,
                        amount, prob, expectedRev,
                        Date.valueOf(today.plusDays(10 + i * 3)),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_deals (id, organization_id, pipeline_id, stage_id, account_id, contact_id, owner_user_id, title, amount, probability, expected_revenue, expected_close_date, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    dealRows);
            counts.put("crmDeals", dealRows.size());
        }
        List<UUID> dealIds = queryIds("SELECT id FROM crm_deals WHERE organization_id = ? AND deleted_at IS NULL", orgId);

        if (countTable("crm_quotes", orgId) == 0 && !dealIds.isEmpty()) {
            List<Object[]> quoteRows = new ArrayList<>(15);
            List<Object[]> quoteLineRows = new ArrayList<>(15);
            List<UUID> quoteIds = new ArrayList<>(15);
            for (int i = 1; i <= 15; i++) {
                UUID qid = UUID.randomUUID();
                quoteIds.add(qid);
                BigDecimal sub = BigDecimal.valueOf(12_500L + (i * 2_200L)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal tax = sub.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal grand = sub.add(tax);
                quoteRows.add(new Object[]{
                        qid, orgId, String.format("QT-D%04d", i),
                        accountIds.isEmpty() ? null : accountIds.get((i - 1) % accountIds.size()),
                        contactIds.isEmpty() ? null : contactIds.get((i - 1) % contactIds.size()),
                        dealIds.get((i - 1) % dealIds.size()),
                        ownerUserId,
                        "Annual SaaS License Proposal #" + i,
                        i % 3 == 0 ? "ACCEPTED" : i % 2 == 0 ? "SENT" : "DRAFT",
                        Date.valueOf(today.plusDays(30)),
                        sub, tax, grand, "Standard enterprise SLA terms",
                        0L, ts, ts, "seeder", "seeder"
                });
                quoteLineRows.add(new Object[]{
                        UUID.randomUUID(), orgId, qid,
                        productIds.isEmpty() ? null : productIds.get((i - 1) % productIds.size()),
                        "Zenius Enterprise Suite Seat Pack",
                        BigDecimal.valueOf(10 + i), sub.divide(BigDecimal.valueOf(10 + i), 2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO, new BigDecimal("18.00"), grand,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_quotes (id, organization_id, quote_number, account_id, contact_id, deal_id, owner_user_id, subject, status, valid_until, subtotal, tax_total, grand_total, notes, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    quoteRows);
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_quote_lines (id, organization_id, quote_id, product_id, item_name, quantity, unit_price, discount_percent, tax_rate, line_total, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    quoteLineRows);
            counts.put("crmQuotes", quoteRows.size());

            List<Object[]> salesOrderRows = new ArrayList<>(10);
            for (int i = 1; i <= 10; i++) {
                salesOrderRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("SO-D%04d", i),
                        quoteIds.get(i - 1),
                        accountIds.isEmpty() ? null : accountIds.get((i - 1) % accountIds.size()),
                        dealIds.get((i - 1) % dealIds.size()),
                        "Confirmed Enterprise Subscription Order #" + i,
                        i % 3 == 0 ? "FULFILLED" : "CONFIRMED",
                        Date.valueOf(today.minusDays(i)),
                        BigDecimal.valueOf(15_000L + (i * 2_500L)),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_sales_orders (id, organization_id, order_number, quote_id, account_id, deal_id, subject, status, order_date, grand_total, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    salesOrderRows);
            counts.put("crmSalesOrders", salesOrderRows.size());
        }

        if (countTable("crm_activities", orgId) == 0 && !dealIds.isEmpty()) {
            String[] actTypes = {"CALL", "MEETING", "TASK", "EMAIL"};
            List<Object[]> actRows = new ArrayList<>(25);
            for (int i = 1; i <= 25; i++) {
                boolean done = i % 3 == 0;
                actRows.add(new Object[]{
                        UUID.randomUUID(), orgId, ownerUserId,
                        actTypes[i % actTypes.length],
                        "Follow-up " + actTypes[i % actTypes.length] + " for Deal #" + i,
                        "Review security checklist, procurement milestones, and onboarding timeline.",
                        "DEAL", dealIds.get((i - 1) % dealIds.size()),
                        Timestamp.from(now.plus(i, ChronoUnit.DAYS)),
                        done ? ts : null,
                        done ? "COMPLETED" : "OPEN",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crm_activities (id, organization_id, owner_user_id, activity_type, subject, description, related_entity_type, related_entity_id, due_at, completed_at, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    actRows);
            counts.put("crmActivities", actRows.size());
        }

        // 6. Marketing Module
        if (countTable("mkt_subscribers", orgId) == 0) {
            List<Object[]> subRows = new ArrayList<>(150);
            for (int i = 1; i <= 150; i++) {
                subRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("sub.%s.%03d@marketing.example.com", tag, i),
                        FIRST_NAMES[(i + 2) % FIRST_NAMES.length] + " " + LAST_NAMES[i % LAST_NAMES.length],
                        i % 2 == 0 ? "WEBINAR" : "NEWSLETTER",
                        i % 3 == 0 ? "VIP,ENTERPRISE" : "GROWTH",
                        i % 15 == 0 ? "UNSUBSCRIBED" : "SUBSCRIBED",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO mkt_subscribers (id, organization_id, email, full_name, source, tags, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    subRows);
            counts.put("marketingSubscribers", subRows.size());
        }

        if (countTable("mkt_segments", orgId) == 0) {
            List<Object[]> segRows = new ArrayList<>();
            String[][] segs = {
                    {"Enterprise Decision Makers", "CXOs and VPs from Fortune 1000 accounts", "VIP", "WEBINAR"},
                    {"SaaS Free Trial Cohort", "Recent product-led growth signups", "GROWTH", "WEBSITE"},
                    {"Renewal Window Q4", "Active subscribers up for annual renewal", "ENTERPRISE", "NEWSLETTER"},
                    {"Webinar Attendees", "Registered attendees from Cloud Summit", "ENTERPRISE", "WEBINAR"},
                    {"APAC Expansion Leads", "High-intent regional inbound pipeline", "GROWTH", "LINKEDIN"}
            };
            for (String[] s : segs) {
                segRows.add(new Object[]{UUID.randomUUID(), orgId, s[0], s[1], s[2], s[3], 0L, ts, ts, "seeder", "seeder"});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO mkt_segments (id, organization_id, name, description, filter_tag, filter_source, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    segRows);
            counts.put("marketingSegments", segRows.size());
        }
        List<UUID> segIds = queryIds("SELECT id FROM mkt_segments WHERE organization_id = ?", orgId);

        if (countTable("mkt_campaigns", orgId) == 0) {
            List<Object[]> campRows = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                boolean sent = i <= 5;
                campRows.add(new Object[]{
                        UUID.randomUUID(), orgId, segIds.isEmpty() ? null : segIds.get((i - 1) % segIds.size()),
                        "Q4 Enterprise Launch Campaign #" + i,
                        i % 3 == 0 ? "SMS" : "EMAIL",
                        "Unlock Autonomous Ops with Zenius One (" + i + ")",
                        "Discover unified CRM, ERP, HR, and AI Workflows across your organization.",
                        sent ? "SENT" : "DRAFT",
                        Timestamp.from(now.plus(i, ChronoUnit.DAYS)),
                        sent ? ts : null,
                        sent ? 120 + (i * 25) : 0,
                        sent ? 78 + (i * 12) : 0,
                        sent ? 34 + (i * 5) : 0,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO mkt_campaigns (id, organization_id, segment_id, name, channel, subject, body_content, status, scheduled_at, sent_at, sent_count, opened_count, clicked_count, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    campRows);
            counts.put("marketingCampaigns", campRows.size());
        }

        if (countTable("mkt_journeys", orgId) == 0) {
            List<Object[]> journeyRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "Enterprise Onboarding Nurture", "LEAD_CREATED", "Welcome Email -> Case Study -> CSM Intro Call", "ACTIVE", 86, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Trial Conversion Accelerator", "TRIAL_STARTED", "Day 1 Setup -> Day 5 AI Tour -> Day 12 Offer", "ACTIVE", 142, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Renewal Engagement Track", "SUBSCRIPTION_EXPIRING", "60-Day Review -> ROI Deck -> Exec Sponsor Note", "ACTIVE", 34, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Re-engagement Winback", "INACTIVE_30D", "Feature Digest -> Discount Coupon -> Survey", "PAUSED", 19, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO mkt_journeys (id, organization_id, name, trigger_event, steps_summary, status, enrolled_count, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    journeyRows);
            counts.put("marketingJourneys", journeyRows.size());
        }

        if (countTable("mkt_forms", orgId) == 0) {
            List<Object[]> formRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "demo-request-form", "Book Live Enterprise Demo", "Request Demo", true, 48, "ACTIVE", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "newsletter-signup", "Monthly Product Dispatch", "Subscribe", false, 115, "ACTIVE", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "partner-application", "Become a Certified Solution Partner", "Apply Now", true, 22, "ACTIVE", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "pricing-inquiry", "Custom Volume Pricing Request", "Get Quote", true, 39, "ACTIVE", 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO mkt_forms (id, organization_id, name, title, submit_label, auto_create_lead, submissions_count, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    formRows);
            counts.put("marketingForms", formRows.size());
        }

        if (countTable("mkt_coupons", orgId) == 0) {
            List<Object[]> couponRows = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                couponRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        "ZENIUS" + (10 * i),
                        BigDecimal.valueOf(5L * i),
                        100, i * 7, Date.valueOf(today.plusMonths(6)), true,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO mkt_coupons (id, organization_id, code, discount_percent, max_redemptions, redemption_count, valid_until, is_active, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    couponRows);
            counts.put("marketingCoupons", couponRows.size());
        }

        // 7. Customer Support (SLAs, Tickets, Comments, Canned Responses, KB, Field Service)
        if (countTable("sup_sla_policies", orgId) == 0) {
            List<Object[]> slaRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "Enterprise Urgent SLA", "URGENT", 15, 240, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Priority High SLA", "HIGH", 60, 480, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Standard Support SLA", "MEDIUM", 240, 1440, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sup_sla_policies (id, organization_id, name, priority, first_response_minutes, resolution_minutes, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    slaRows);
            counts.put("supportSlas", slaRows.size());
        }

        if (countTable("sup_tickets", orgId) == 0) {
            List<Object[]> ticketRows = new ArrayList<>(120);
            for (int i = 1; i <= 120; i++) {
                String status = TICKET_STATUSES[i % TICKET_STATUSES.length];
                boolean resolved = "RESOLVED".equals(status) || "CLOSED".equals(status);
                ticketRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("TKT-D%04d", i),
                        "Customer Support Case #" + i + ": Configuration Inquiry",
                        "Detailed enterprise configuration diagnostics and SLA workflow check #" + i,
                        CHANNELS[i % CHANNELS.length],
                        PRIORITIES[i % PRIORITIES.length],
                        status,
                        FIRST_NAMES[i % FIRST_NAMES.length] + " Customer",
                        String.format("requester.%03d@client.example.com", i),
                        Timestamp.from(now.plus(24, ChronoUnit.HOURS)),
                        resolved ? Timestamp.from(now.minus(2, ChronoUnit.HOURS)) : null,
                        false,
                        resolved ? 4 + (i % 2) : null,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sup_tickets (id, organization_id, ticket_number, subject, description, channel, priority, status, requester_name, requester_email, sla_due_at, resolved_at, escalated, csat_rating, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    ticketRows);
            counts.put("supportTickets", ticketRows.size());
        }
        List<UUID> ticketIds = queryIds("SELECT id FROM sup_tickets WHERE organization_id = ?", orgId);

        if (countTable("sup_ticket_comments", orgId) == 0 && !ticketIds.isEmpty()) {
            List<Object[]> commentRows = new ArrayList<>(25);
            for (int i = 1; i <= 25; i++) {
                commentRows.add(new Object[]{
                        UUID.randomUUID(), orgId, ticketIds.get((i - 1) % ticketIds.size()),
                        ownerUserId, "Support Specialist",
                        "Verified tenant telemetry and applied recommended configuration patch #" + i,
                        i % 4 == 0,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sup_ticket_comments (id, organization_id, ticket_id, author_user_id, author_name, body, internal_note, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    commentRows);
            counts.put("supportTicketComments", commentRows.size());
        }

        if (countTable("sup_canned_responses", orgId) == 0) {
            List<Object[]> canned = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "SSO SAML Metadata Setup", "AUTH", "Upload your IdP metadata XML under Org Admin -> Security Policy to activate SSO.", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Invoice Tax Exemption Update", "BILLING", "Please reply with your W-9 / GST certificate so Finance can apply tax exemption.", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "API Rate Limit Guidance", "API", "Enterprise plans support 5,000 req/min per API key with burst token bucket.", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Payroll Cut-Off Schedule", "PAYROLL", "Monthly payroll runs lock on the 25th of every month prior to disbursement.", 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sup_canned_responses (id, organization_id, title, category, body, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    canned);
            counts.put("supportCannedResponses", canned.size());
        }

        if (countTable("sup_kb_articles", orgId) == 0) {
            List<Object[]> kbRows = new ArrayList<>();
            String[][] articles = {
                    {"Getting Started with Multi-Organization RBAC", "Security", "Configure custom roles, permission keys, and tenant switching cleanly."},
                    {"Configuring GST & TDS Tax Rules in Finance", "Finance", "Step-by-step setup for chart of accounts, tax rules, and journals."},
                    {"Automating Lead Assignment Workflows", "Automation", "Route high-score CRM leads automatically via Workflow Engine triggers."},
                    {"WorkDrive Document Versioning & e-Sign", "Workplace", "Track revision history and issue cryptographic e-Signature requests."},
                    {"Employee Attendance & Leave Policy Guide", "People", "Manage geo-fenced check-ins, holiday calendars, and leave approvals."},
                    {"Setting Up Low-Code Creator Custom Forms", "Creator", "Build custom internal forms and dashboards without writing boilerplate code."}
            };
            for (int i = 0; i < articles.length; i++) {
                kbRows.add(new Object[]{
                        UUID.randomUUID(), orgId, articles[i][0], articles[i][1], articles[i][2],
                        true, 12 + (i * 5), 0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sup_kb_articles (id, organization_id, title, category, content, published, helpful_count, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    kbRows);
            counts.put("supportKbArticles", kbRows.size());
        }

        if (countTable("sup_service_appointments", orgId) == 0 && !ticketIds.isEmpty()) {
            List<Object[]> apptRows = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                apptRows.add(new Object[]{
                        UUID.randomUUID(), orgId, ticketIds.get(i - 1),
                        String.format("FSA-D%04d", i),
                        "Enterprise Client " + String.format("%03d", i),
                        (100 + i) + " Market Street, " + CITIES[i % CITIES.length],
                        ownerUserId,
                        Timestamp.from(now.plus(i * 6L, ChronoUnit.HOURS)),
                        i % 2 == 0 ? "COMPLETED" : "SCHEDULED",
                        "On-site network & workstation rollout inspection #" + i,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sup_service_appointments (id, organization_id, ticket_id, appointment_number, customer_name, service_address, technician_user_id, scheduled_start, status, notes, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    apptRows);
            counts.put("supportAppointments", apptRows.size());
        }

        // 8. Finance, Accounting & Billing
        if (countTable("fin_accounts", orgId) == 0) {
            String[][] coa = {
                    {"1000", "Operating Cash & Bank", "ASSET"},
                    {"1200", "Accounts Receivable", "ASSET"},
                    {"2000", "Accounts Payable", "LIABILITY"},
                    {"2200", "GST & Tax Payable", "LIABILITY"},
                    {"3000", "Retained Earnings", "EQUITY"},
                    {"4000", "SaaS Subscription Revenue", "REVENUE"},
                    {"4100", "Professional Services Revenue", "REVENUE"},
                    {"5000", "Cloud Infrastructure & Payroll", "EXPENSE"}
            };
            List<Object[]> coaRows = new ArrayList<>();
            for (String[] a : coa) {
                coaRows.add(new Object[]{UUID.randomUUID(), orgId, a[0], a[1], a[2], "USD", true, true, 0L, ts, ts, "seeder", "seeder"});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO fin_accounts (id, organization_id, code, name, account_type, currency, is_system, is_active, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    coaRows);
            counts.put("financeAccounts", coaRows.size());
        }
        List<UUID> finAcctIds = queryIds("SELECT id FROM fin_accounts WHERE organization_id = ? ORDER BY code ASC", orgId);

        if (countTable("fin_journal_entries", orgId) == 0 && finAcctIds.size() >= 2) {
            List<Object[]> jRows = new ArrayList<>();
            List<Object[]> jlRows = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                UUID jid = UUID.randomUUID();
                BigDecimal amt = BigDecimal.valueOf(5_000L * i).setScale(2, RoundingMode.HALF_UP);
                jRows.add(new Object[]{
                        jid, orgId, String.format("JE-D%04d", i), Date.valueOf(today.minusDays(i * 3L)),
                        "MONTHLY-CLOSE-" + i, "Automated Revenue & Expense Posting #" + i,
                        "POSTED", amt, amt, 0L, ts, ts, "seeder", "seeder"
                });
                jlRows.add(new Object[]{
                        UUID.randomUUID(), orgId, jid, finAcctIds.get(0),
                        "Cash Settlement Debit", amt, BigDecimal.ZERO, 0L, ts, ts, "seeder", "seeder"
                });
                jlRows.add(new Object[]{
                        UUID.randomUUID(), orgId, jid, finAcctIds.get(finAcctIds.size() - 3),
                        "SaaS Revenue Recognition Credit", BigDecimal.ZERO, amt, 0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO fin_journal_entries (id, organization_id, entry_number, entry_date, reference, description, status, total_debit, total_credit, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    jRows);
            jdbcTemplate.batchUpdate(
                    "INSERT INTO fin_journal_lines (id, organization_id, journal_entry_id, account_id, description, debit_amount, credit_amount, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    jlRows);
            counts.put("financeJournals", jRows.size());
        }

        if (countTable("fin_invoices", orgId) == 0) {
            List<Object[]> invoiceRows = new ArrayList<>(50);
            for (int i = 1; i <= 50; i++) {
                BigDecimal sub = BigDecimal.valueOf(1_200L + (i * 350L)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal gst = sub.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = sub.add(gst);
                boolean paid = i % 2 == 0;
                invoiceRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("INV-D%04d", i),
                        "Enterprise Client " + String.format("%03d", ((i - 1) % 50) + 1),
                        String.format("ap.%03d@client.example.com", i),
                        "USD",
                        Date.valueOf(today.minusDays(i % 45)),
                        Date.valueOf(today.plusDays(30 - (i % 45))),
                        sub,
                        new BigDecimal("18.00"), gst,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        total,
                        paid ? BigDecimal.ZERO : total,
                        paid ? "PAID" : (i % 5 == 0 ? "OVERDUE" : "SENT"),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO fin_invoices (id, organization_id, invoice_number, customer_name, customer_email, currency, issue_date, due_date, subtotal, gst_rate, gst_amount, tds_rate, tds_amount, total_amount, balance_due, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    invoiceRows);
            counts.put("financeInvoices", invoiceRows.size());
        }
        List<UUID> invIds = queryIds("SELECT id FROM fin_invoices WHERE organization_id = ?", orgId);

        if (countTable("fin_bills", orgId) == 0) {
            List<Object[]> billRows = new ArrayList<>(15);
            for (int i = 1; i <= 15; i++) {
                BigDecimal sub = BigDecimal.valueOf(900L + (i * 250L)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal tds = sub.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal net = sub.subtract(tds);
                boolean paid = i % 3 == 0;
                billRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("BILL-D%04d", i),
                        "Cloud & SaaS Vendor #" + i,
                        Date.valueOf(today.minusDays(i * 2L)),
                        Date.valueOf(today.plusDays(25 - i)),
                        sub, new BigDecimal("10.00"), tds, net,
                        paid ? BigDecimal.ZERO : net,
                        paid ? "PAID" : "OPEN",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO fin_bills (id, organization_id, bill_number, vendor_name, bill_date, due_date, subtotal, tds_rate, tds_amount, total_amount, balance_due, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    billRows);
            counts.put("financeBills", billRows.size());
        }

        if (countTable("fin_payments", orgId) == 0 && !invIds.isEmpty()) {
            List<Object[]> payRows = new ArrayList<>(12);
            for (int i = 1; i <= 12; i++) {
                payRows.add(new Object[]{
                        UUID.randomUUID(), orgId, "INVOICE_RECEIPT",
                        invIds.get((i - 1) % invIds.size()),
                        Date.valueOf(today.minusDays(i)),
                        BigDecimal.valueOf(2_000L + (i * 350L)),
                        i % 2 == 0 ? "ACH_WIRE" : "STRIPE_CARD",
                        "Automated settlement reference PAY-" + i,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO fin_payments (id, organization_id, payment_type, reference_id, payment_date, amount, method, reference_note, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    payRows);
            counts.put("financePayments", payRows.size());
        }

        if (countTable("bill_plans", orgId) == 0) {
            List<Object[]> planRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "STARTER-M", "Zenius Starter Monthly", "MONTHLY", new BigDecimal("49.00"), "USD", 14, true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "GROWTH-M", "Zenius Growth Suite", "MONTHLY", new BigDecimal("199.00"), "USD", 14, true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "ENT-ANNUAL", "Zenius Enterprise One", "ANNUAL", new BigDecimal("1999.00"), "USD", 30, true, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO bill_plans (id, organization_id, code, name, billing_interval, price, currency, trial_days, is_active, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    planRows);
            counts.put("billingPlans", planRows.size());
        }
        List<UUID> planIds = queryIds("SELECT id FROM bill_plans WHERE organization_id = ?", orgId);

        if (countTable("bill_subscriptions", orgId) == 0 && !planIds.isEmpty()) {
            List<Object[]> subRows = new ArrayList<>(12);
            for (int i = 1; i <= 12; i++) {
                subRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("SUB-D%04d", i),
                        planIds.get((i - 1) % planIds.size()),
                        "Enterprise Client " + String.format("%03d", i),
                        String.format("billing.%03d@client.example.com", i),
                        i % 6 == 0 ? "PAST_DUE" : "ACTIVE",
                        Date.valueOf(today.minusDays(15)),
                        Date.valueOf(today.plusDays(15)),
                        BigDecimal.ZERO,
                        i % 6 == 0 ? 1 : 0,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO bill_subscriptions (id, organization_id, subscription_number, plan_id, customer_name, customer_email, status, current_period_start, current_period_end, credit_balance, dunning_attempts, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    subRows);
            counts.put("billingSubscriptions", subRows.size());
        }

        // 9. Inventory & Commerce
        if (countTable("inv_warehouses", orgId) == 0) {
            List<Object[]> whRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "WH-NYC", "East Coast Distribution Hub", "New York", true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "WH-BLR", "APAC Fulfillment Center", "Bengaluru", false, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "WH-LDN", "EMEA Logistics Depot", "London", false, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO inv_warehouses (id, organization_id, code, name, city, is_default, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    whRows);
            counts.put("inventoryWarehouses", whRows.size());
        }
        List<UUID> whIds = queryIds("SELECT id FROM inv_warehouses WHERE organization_id = ?", orgId);

        if (countTable("inv_items", orgId) == 0) {
            List<Object[]> itemRows = new ArrayList<>(70);
            for (int i = 1; i <= 70; i++) {
                itemRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("INV-%s-%03d", tag.toUpperCase(), i),
                        "Hardware / Supply SKU #" + i,
                        i % 2 == 0 ? "HARDWARE" : "ACCESSORIES",
                        BigDecimal.valueOf(45 + (i * 8)),
                        BigDecimal.valueOf(80 + (i * 15)),
                        15,
                        i % 3 == 0,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO inv_items (id, organization_id, sku, name, category, unit_cost, selling_price, reorder_level, track_serial, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    itemRows);
            counts.put("inventoryItems", itemRows.size());
        }
        List<UUID> itemIds = queryIds("SELECT id FROM inv_items WHERE organization_id = ?", orgId);

        if (countTable("inv_stock_levels", orgId) == 0 && !whIds.isEmpty() && !itemIds.isEmpty()) {
            List<Object[]> stockRows = new ArrayList<>(30);
            int limit = Math.min(30, itemIds.size());
            for (int i = 0; i < limit; i++) {
                stockRows.add(new Object[]{
                        UUID.randomUUID(), orgId, itemIds.get(i), whIds.get(i % whIds.size()),
                        25 + (i * 4), 0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO inv_stock_levels (id, organization_id, item_id, warehouse_id, quantity_on_hand, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    stockRows);
            counts.put("inventoryStockLevels", stockRows.size());
        }

        if (countTable("inv_purchase_orders", orgId) == 0 && !whIds.isEmpty() && !itemIds.isEmpty()) {
            List<Object[]> poRows = new ArrayList<>(12);
            for (int i = 1; i <= 12; i++) {
                BigDecimal unit = BigDecimal.valueOf(60 + (i * 10));
                int qty = 20 + i * 5;
                poRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("PO-D%04d", i),
                        "Global Hardware Supplier #" + i,
                        whIds.get(i % whIds.size()), itemIds.get(i % itemIds.size()),
                        qty, unit, unit.multiply(BigDecimal.valueOf(qty)),
                        i % 3 == 0 ? "RECEIVED" : "ORDERED",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO inv_purchase_orders (id, organization_id, po_number, vendor_name, warehouse_id, item_id, quantity, unit_cost, total_cost, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    poRows);
            counts.put("inventoryPurchaseOrders", poRows.size());
        }

        if (countTable("inv_store_orders", orgId) == 0 && !whIds.isEmpty() && !itemIds.isEmpty()) {
            List<Object[]> soRows = new ArrayList<>(15);
            for (int i = 1; i <= 15; i++) {
                int qty = 2 + (i % 5);
                soRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("STR-D%04d", i),
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[i % LAST_NAMES.length],
                        String.format("store.%03d@client.example.com", i),
                        whIds.get(i % whIds.size()), itemIds.get(i % itemIds.size()),
                        qty, BigDecimal.valueOf(190L * qty),
                        i % 2 == 0 ? "SHIPPED" : "PAID",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO inv_store_orders (id, organization_id, order_number, customer_name, customer_email, warehouse_id, item_id, quantity, total_amount, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    soRows);
            counts.put("inventoryStoreOrders", soRows.size());
        }

        // 10. People, HR, Recruitment & Payroll
        if (countTable("hr_employees", orgId) == 0) {
            List<Object[]> empRows = new ArrayList<>(60);
            for (int i = 1; i <= 60; i++) {
                empRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("EMP-D%03d", i),
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[(i * 5) % LAST_NAMES.length],
                        String.format("staff.%s.%03d@zeniusone.local", tag, i),
                        i % 4 == 0 ? "Senior Engineer" : i % 3 == 0 ? "Account Executive" : "Product Specialist",
                        DEPARTMENTS[i % DEPARTMENTS.length],
                        "Headquarters",
                        Date.valueOf(today.minusMonths(1 + (i % 24))),
                        "FULL_TIME",
                        "ACTIVE",
                        BigDecimal.valueOf(4_500L + (i * 150L)),
                        12 + (i % 10),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO hr_employees (id, organization_id, employee_code, full_name, work_email, designation, department_name, branch_name, joining_date, employment_type, status, base_monthly_salary, annual_leave_balance, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    empRows);
            counts.put("hrEmployees", empRows.size());
        }
        List<UUID> empIds = queryIds("SELECT id FROM hr_employees WHERE organization_id = ? ORDER BY employee_code ASC", orgId);

        if (countTable("hr_attendance_records", orgId) == 0 && !empIds.isEmpty()) {
            List<Object[]> attRows = new ArrayList<>(30);
            int limit = Math.min(30, empIds.size());
            for (int i = 0; i < limit; i++) {
                attRows.add(new Object[]{
                        UUID.randomUUID(), orgId, empIds.get(i), Date.valueOf(today),
                        Timestamp.from(now.minus(8, ChronoUnit.HOURS)), ts,
                        i % 3 == 0 ? "REMOTE" : "OFFICE", 40.7128, -74.0060,
                        480 + (i % 4) * 15, (i % 4) * 15, "PRESENT",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO hr_attendance_records (id, organization_id, employee_id, work_date, check_in_at, check_out_at, work_mode, latitude, longitude, worked_minutes, overtime_minutes, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    attRows);
            counts.put("hrAttendance", attRows.size());
        }

        if (countTable("hr_leave_requests", orgId) == 0 && !empIds.isEmpty()) {
            List<Object[]> leaveRows = new ArrayList<>(12);
            for (int i = 1; i <= 12; i++) {
                boolean approved = i % 3 == 0;
                leaveRows.add(new Object[]{
                        UUID.randomUUID(), orgId, empIds.get((i - 1) % empIds.size()),
                        i % 2 == 0 ? "ANNUAL" : "SICK",
                        Date.valueOf(today.plusDays(i * 2L)),
                        Date.valueOf(today.plusDays(i * 2L + 2)),
                        3, "Scheduled personal PTO #" + i,
                        approved ? "APPROVED" : "PENDING",
                        approved ? ownerUserId : null,
                        approved ? ts : null,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO hr_leave_requests (id, organization_id, employee_id, leave_type, start_date, end_date, days_count, reason, status, reviewed_by_user_id, reviewed_at, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    leaveRows);
            counts.put("hrLeaveRequests", leaveRows.size());
        }

        if (countTable("hr_performance_goals", orgId) == 0 && !empIds.isEmpty()) {
            List<Object[]> goalRows = new ArrayList<>(20);
            for (int i = 1; i <= 20; i++) {
                int pct = Math.min(100, 35 + i * 3);
                goalRows.add(new Object[]{
                        UUID.randomUUID(), orgId, empIds.get((i - 1) % empIds.size()),
                        "Q4 OKR Milestone #" + i,
                        "99.9% SLA / KPI Delivery",
                        pct, pct >= 90 ? "COMPLETED" : "IN_PROGRESS",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO hr_performance_goals (id, organization_id, employee_id, title, target_metric, progress_percent, review_status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    goalRows);
            counts.put("hrPerformanceGoals", goalRows.size());
        }

        if (countTable("rec_jobs", orgId) == 0) {
            List<Object[]> jobRows = new ArrayList<>(8);
            String[] titles = {"Principal Java Architect", "Senior React Engineer", "Enterprise AE", "Product Designer", "Site Reliability Lead", "AI Solutions Engineer", "HR Business Partner", "Security Analyst"};
            for (int i = 1; i <= 8; i++) {
                jobRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("JOB-D%03d", i),
                        titles[i - 1], DEPARTMENTS[i % DEPARTMENTS.length],
                        CITIES[i % CITIES.length], "FULL_TIME", 1 + (i % 3), "OPEN",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO rec_jobs (id, organization_id, job_code, title, department, location, employment_type, openings_count, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    jobRows);
            counts.put("recruitmentJobs", jobRows.size());
        }
        List<UUID> jobIds = queryIds("SELECT id FROM rec_jobs WHERE organization_id = ?", orgId);

        if (countTable("rec_candidates", orgId) == 0 && !jobIds.isEmpty()) {
            String[] stages = {"APPLIED", "SCREENING", "INTERVIEW", "OFFER"};
            List<Object[]> candRows = new ArrayList<>(24);
            for (int i = 1; i <= 24; i++) {
                candRows.add(new Object[]{
                        UUID.randomUUID(), orgId, jobIds.get((i - 1) % jobIds.size()),
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[(i * 2) % LAST_NAMES.length] + " (Cand #" + i + ")",
                        String.format("candidate.%s.%03d@talent.example.com", tag, i),
                        String.format("+1-555-08%02d", i),
                        "Java 21, Spring Boot, React, TypeScript, Cloud",
                        3 + (i % 8),
                        "Experienced enterprise software professional ready for technical round.",
                        stages[i % stages.length],
                        3 + (i % 3),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO rec_candidates (id, organization_id, job_id, full_name, email, phone, skills, experience_years, resume_summary, stage, rating, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    candRows);
            counts.put("recruitmentCandidates", candRows.size());
        }
        List<UUID> candIds = queryIds("SELECT id FROM rec_candidates WHERE organization_id = ?", orgId);

        if (countTable("rec_interviews", orgId) == 0 && !candIds.isEmpty()) {
            List<Object[]> ivRows = new ArrayList<>(10);
            for (int i = 1; i <= 10; i++) {
                ivRows.add(new Object[]{
                        UUID.randomUUID(), orgId, candIds.get((i - 1) % candIds.size()),
                        "Aarav Mehta", Timestamp.from(now.plus(i * 4L, ChronoUnit.HOURS)),
                        i % 2 == 0 ? "VIDEO" : "ONSITE",
                        "Strong system design & problem-solving fundamentals.",
                        i % 3 == 0 ? "STRONG_HIRE" : "PENDING",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO rec_interviews (id, organization_id, candidate_id, interviewer_name, scheduled_at, mode, feedback, recommendation, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    ivRows);
            counts.put("recruitmentInterviews", ivRows.size());
        }

        if (countTable("pay_runs", orgId) == 0 && !empIds.isEmpty()) {
            String prevMonth = today.minusMonths(1).toString().substring(0, 7);
            String curMonth = today.toString().substring(0, 7);
            UUID run1 = UUID.randomUUID();
            UUID run2 = UUID.randomUUID();
            jdbcTemplate.batchUpdate(
                    "INSERT INTO pay_runs (id, organization_id, pay_month, employees_count, total_gross, total_deductions, total_net_pay, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(
                            new Object[]{run1, orgId, prevMonth, 30, new BigDecimal("210000.00"), new BigDecimal("35700.00"), new BigDecimal("174300.00"), "PAID", 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{run2, orgId, curMonth, 30, new BigDecimal("216000.00"), new BigDecimal("36720.00"), new BigDecimal("179280.00"), "PROCESSED", 0L, ts, ts, "seeder", "seeder"}
                    ));
            List<Object[]> slipRows = new ArrayList<>(30);
            int limit = Math.min(30, empIds.size());
            for (int i = 0; i < limit; i++) {
                BigDecimal gross = BigDecimal.valueOf(5_000L + (i * 150L)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal basic = gross.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal hra = gross.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal special = gross.subtract(basic).subtract(hra);
                BigDecimal pf = basic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal esi = BigDecimal.ZERO;
                BigDecimal tds = gross.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalDed = pf.add(esi).add(tds);
                slipRows.add(new Object[]{
                        UUID.randomUUID(), orgId, run2, empIds.get(i),
                        FIRST_NAMES[(i + 1) % FIRST_NAMES.length] + " " + LAST_NAMES[((i + 1) * 5) % LAST_NAMES.length],
                        curMonth, basic, hra, special, gross, pf, esi, tds, totalDed, gross.subtract(totalDed),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO pay_payslips (id, organization_id, payroll_run_id, employee_id, employee_name, pay_month, basic_pay, hra_allowance, special_allowance, gross_pay, pf_deduction, esi_deduction, tds_tax_deduction, total_deductions, net_pay, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    slipRows);
            counts.put("payrollRuns", 2);
            counts.put("payrollPayslips", slipRows.size());
        }

        // 11. Projects, Sprints, Tasks & Time Logs
        if (countTable("proj_projects", orgId) == 0) {
            List<Object[]> projectRows = new ArrayList<>(10);
            for (int i = 1; i <= 10; i++) {
                projectRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("PRJ-D%03d", i),
                        "Enterprise Rollout Initiative #" + i,
                        "Enterprise Client " + String.format("%03d", i),
                        "ACTIVE",
                        BigDecimal.valueOf(30_000L + (i * 5_000L)),
                        Date.valueOf(today.minusDays(15)),
                        Date.valueOf(today.plusDays(60)),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO proj_projects (id, organization_id, project_code, name, client_name, status, budget_amount, start_date, due_date, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    projectRows);
            counts.put("projects", projectRows.size());
        }
        List<UUID> projectIds = queryIds("SELECT id FROM proj_projects WHERE organization_id = ? ORDER BY project_code ASC", orgId);

        if (countTable("proj_sprints", orgId) == 0 && !projectIds.isEmpty()) {
            List<Object[]> sprintRows = new ArrayList<>(projectIds.size());
            for (int i = 0; i < projectIds.size(); i++) {
                sprintRows.add(new Object[]{
                        UUID.randomUUID(), orgId, projectIds.get(i),
                        "Sprint " + (i + 1) + " — Core Delivery",
                        "Ship microservice API contracts and verified frontend views",
                        Date.valueOf(today.minusDays(7)), Date.valueOf(today.plusDays(7)),
                        "ACTIVE", 0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO proj_sprints (id, organization_id, project_id, name, goal, start_date, end_date, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    sprintRows);
            counts.put("projectSprints", sprintRows.size());
        }

        if (countTable("proj_tasks", orgId) == 0 && !projectIds.isEmpty()) {
            List<Object[]> taskRows = new ArrayList<>(60);
            for (int i = 1; i <= 60; i++) {
                int pIdx = (i - 1) % projectIds.size();
                taskRows.add(new Object[]{
                        UUID.randomUUID(), orgId, projectIds.get(pIdx),
                        String.format("PRJ-D%03d-T%d", pIdx + 1, i),
                        "Deliverable Milestone Task #" + i,
                        i % 5 == 0 ? "BUG" : "TASK",
                        PRIORITIES[i % PRIORITIES.length],
                        TASK_STATUSES[i % TASK_STATUSES.length],
                        1 + (i % 5),
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[i % LAST_NAMES.length],
                        Date.valueOf(today.plusDays(5 + (i % 25))),
                        (i % 4) * 45,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO proj_tasks (id, organization_id, project_id, task_key, title, task_type, priority, status, story_points, assignee_name, due_date, logged_minutes, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    taskRows);
            counts.put("projectTasks", taskRows.size());
        }
        List<UUID> taskIds = queryIds("SELECT id FROM proj_tasks WHERE organization_id = ?", orgId);

        if (countTable("proj_time_logs", orgId) == 0 && !projectIds.isEmpty() && !taskIds.isEmpty()) {
            List<Object[]> tlRows = new ArrayList<>(25);
            for (int i = 1; i <= 25; i++) {
                tlRows.add(new Object[]{
                        UUID.randomUUID(), orgId, projectIds.get((i - 1) % projectIds.size()),
                        taskIds.get((i - 1) % taskIds.size()),
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[i % LAST_NAMES.length],
                        60 + (i % 4) * 30, true, Date.valueOf(today.minusDays(i % 10)),
                        "Implementation, unit testing & code review session #" + i,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO proj_time_logs (id, organization_id, project_id, task_id, member_name, minutes_logged, billable, work_date, notes, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tlRows);
            counts.put("projectTimeLogs", tlRows.size());
        }

        // 12. Business Mail, Channels, Messages & Meetings
        if (countTable("col_mail_messages", orgId) == 0) {
            List<Object[]> mailRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "INBOX", "cfo@acmeglobal.example.com", "admin@zeniusone.local", "Q4 Enterprise Budget & Cloud Consolidation Approval", "Hi Aarav,\n\nPlease review the attached Q4 operating budget in WorkDrive and sign the MSA before Friday.", "FINANCE", false, true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "INBOX", "vp.sales@zeniusone.local", "admin@zeniusone.local", "Pipeline Review: 5 Enterprise Deals in Final Negotiation", "Team,\n\nTotal pipeline weighted forecast crossed $640k this week. Check the Sales Kanban for contract status.", "SALES", false, false, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "INBOX", "security@zeniusone.local", "admin@zeniusone.local", "SOC2 Audit Readiness — Monthly Check Passed", "All 20 endpoints passed compliance checks. Review latest audit log metrics in IT Security.", "SECURITY", true, true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "INBOX", "hr.ops@zeniusone.local", "admin@zeniusone.local", "Payroll Cycle Confirmation for Current Month", "Monthly payroll batch for 30 active employees is ready for disbursement verification.", "HR", false, false, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "SENT", "admin@zeniusone.local", "legal@client.example.com", "MSA Renewal & DPA Signature Dispatch", "Hi Legal Team,\n\nSharing the updated Master Services Agreement via Zenius Sign.", "LEGAL", true, false, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO col_mail_messages (id, organization_id, folder, sender_email, recipient_email, subject, body, labels, is_read, starred, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    mailRows);
            counts.put("collaborationMail", mailRows.size());
        }

        if (countTable("col_channels", orgId) == 0) {
            List<Object[]> chRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "general", "Company-wide announcements and executive updates", false, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "engineering", "Architecture, microservices & release train", false, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "enterprise-deals", "Deal desk and executive sponsorships", false, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "support-escalations", "Urgent customer SLA triage", true, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO col_channels (id, organization_id, name, description, is_private, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    chRows);
            counts.put("collaborationChannels", chRows.size());
        }
        List<UUID> chanIds = queryIds("SELECT id FROM col_channels WHERE organization_id = ?", orgId);

        if (countTable("col_messages", orgId) == 0 && !chanIds.isEmpty()) {
            List<Object[]> msgRows = new ArrayList<>();
            for (int i = 1; i <= 16; i++) {
                msgRows.add(new Object[]{
                        UUID.randomUUID(), orgId, chanIds.get((i - 1) % chanIds.size()),
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[i % LAST_NAMES.length],
                        "Team update #" + i + ": Milestone verified and synced with Zenius Projects.",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO col_messages (id, organization_id, channel_id, sender_name, body, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    msgRows);
            counts.put("collaborationMessages", msgRows.size());
        }

        if (countTable("col_meetings", orgId) == 0) {
            List<Object[]> mtgRows = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                mtgRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        "Executive Sync #" + i + " — Operations & Product",
                        "Aarav Mehta",
                        Timestamp.from(now.plus(i * 3L, ChronoUnit.HOURS)),
                        30 + (i % 2) * 15,
                        "https://meet.zeniusone.local/exec-" + tag + "-" + i,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO col_meetings (id, organization_id, title, organizer_name, scheduled_at, duration_minutes, meeting_link, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    mtgRows);
            counts.put("collaborationMeetings", mtgRows.size());
        }

        // 13. WorkDrive Folders, Office Files, Revisions, e-Signatures & Legal Contracts
        if (countTable("doc_folders", orgId) == 0) {
            List<Object[]> folderRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, null, "Shared Drive", true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, null, "Finance & Tax Reports", true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, null, "Engineering Architecture", true, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, null, "Legal & MSAs", false, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO doc_folders (id, organization_id, parent_folder_id, name, shared, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    folderRows);
            counts.put("workdriveFolders", folderRows.size());
        }
        List<UUID> folderIds = queryIds("SELECT id FROM doc_folders WHERE organization_id = ?", orgId);

        if (countTable("doc_files", orgId) == 0 && !folderIds.isEmpty()) {
            String[] docTypes = {"WORD", "SHEET", "SLIDE", "PDF"};
            List<Object[]> fileRows = new ArrayList<>(8);
            for (int i = 1; i <= 8; i++) {
                fileRows.add(new Object[]{
                        UUID.randomUUID(), orgId, folderIds.get((i - 1) % folderIds.size()),
                        "Zenius Enterprise Deliverable Document #" + i,
                        docTypes[i % docTypes.length],
                        "# Deliverable #" + i + "\n\nExecutive summary, milestones, SLA commitments, and compliance controls for Acme Global Corp.",
                        1, "STANDARD_7Y",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO doc_files (id, organization_id, folder_id, title, doc_type, content_body, current_version, retention_policy, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    fileRows);
            counts.put("workdriveFiles", fileRows.size());
        }
        List<UUID> fileIds = queryIds("SELECT id FROM doc_files WHERE organization_id = ?", orgId);

        if (countTable("doc_revisions", orgId) == 0 && !fileIds.isEmpty()) {
            List<Object[]> revRows = new ArrayList<>();
            for (UUID fid : fileIds) {
                revRows.add(new Object[]{
                        UUID.randomUUID(), orgId, fid, 1, "Aarav Mehta",
                        "Initial approved baseline content for compliance archive.",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO doc_revisions (id, organization_id, document_id, revision_number, editor_name, content_snapshot, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    revRows);
            counts.put("workdriveRevisions", revRows.size());
        }

        if (countTable("doc_signature_requests", orgId) == 0 && !fileIds.isEmpty()) {
            List<Object[]> sigRows = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                boolean signed = i % 2 == 0;
                sigRows.add(new Object[]{
                        UUID.randomUUID(), orgId, fileIds.get((i - 1) % fileIds.size()),
                        FIRST_NAMES[i] + " Signer", String.format("signer%d@client.example.com", i),
                        signed ? "SIGNED" : "PENDING",
                        signed ? "sha256-" + UUID.randomUUID().toString().replace("-", "") : null,
                        signed ? ts : null,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO doc_signature_requests (id, organization_id, document_id, signer_name, signer_email, status, signature_hash, signed_at, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    sigRows);
            counts.put("workdriveSignatures", sigRows.size());
        }

        if (countTable("leg_contracts", orgId) == 0 && !fileIds.isEmpty()) {
            String[] ctypes = {"MSA", "NDA", "SOW", "DPA"};
            List<Object[]> ctrRows = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                ctrRows.add(new Object[]{
                        UUID.randomUUID(), orgId, String.format("CTR-D%04d", i),
                        "Enterprise Master Agreement #" + i,
                        "Enterprise Client " + String.format("%03d", i),
                        ctypes[i % ctypes.length],
                        BigDecimal.valueOf(50_000L + (i * 15_000L)),
                        Date.valueOf(today.minusMonths(2)),
                        Date.valueOf(today.plusMonths(10)),
                        i % 2 == 0 ? "ACTIVE" : "UNDER_REVIEW",
                        fileIds.get((i - 1) % fileIds.size()),
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO leg_contracts (id, organization_id, contract_number, title, counterparty_name, contract_type, value_amount, effective_date, expiration_date, status, document_id, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    ctrRows);
            counts.put("legalContracts", ctrRows.size());
        }

        // 14. Analytics & BI
        if (countTable("bi_dashboards", orgId) == 0) {
            List<Object[]> dashRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "Executive Revenue & Pipeline Pulse", "Cross-suite CRM, Billing, and Invoice cashflow metrics", true, "[]", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Support SLA & CSAT Command", "Live ticket volume, escalation rate, and resolution velocity", true, "[]", 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Workforce & Utilization Overview", "Headcount by department, payroll burn, and project hours", false, "[]", 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO bi_dashboards (id, organization_id, name, description, shared, layout_json, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    dashRows);
            counts.put("analyticsDashboards", dashRows.size());
        }
        List<UUID> dashIds = queryIds("SELECT id FROM bi_dashboards WHERE organization_id = ?", orgId);

        if (countTable("bi_reports", orgId) == 0 && !dashIds.isEmpty()) {
            String[][] reports = {
                    {"Leads by Lifecycle Source", "CRM_LEADS", "BAR", "COUNT", "SOURCE"},
                    {"Invoices by Settlement Status", "FINANCE_INVOICES", "PIE", "SUM_AMOUNT", "STATUS"},
                    {"Support Tickets by Priority", "SUPPORT_TICKETS", "BAR", "COUNT", "PRIORITY"},
                    {"Employees by Department", "HR_EMPLOYEES", "DONUT", "COUNT", "DEPARTMENT"},
                    {"Tasks by Delivery Stage", "PROJECT_TASKS", "BAR", "COUNT", "STATUS"},
                    {"IT Assets Compliance Health", "IT_ASSETS", "BAR", "COUNT", "STATUS"}
            };
            List<Object[]> repRows = new ArrayList<>();
            for (int i = 0; i < reports.length; i++) {
                repRows.add(new Object[]{
                        UUID.randomUUID(), orgId, dashIds.get(i % dashIds.size()),
                        reports[i][0], reports[i][1], reports[i][2], reports[i][3], reports[i][4],
                        "{}", 0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO bi_reports (id, organization_id, dashboard_id, name, data_source, chart_type, metric, dimension, config_json, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    repRows);
            counts.put("analyticsReports", repRows.size());
        }

        // 15. Low-Code Creator
        if (countTable("crt_apps", orgId) == 0) {
            UUID app1 = UUID.randomUUID();
            UUID app2 = UUID.randomUUID();
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crt_apps (id, organization_id, app_code, name, description, status, app_version, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(
                            new Object[]{app1, orgId, "CAPEX-APPROVAL", "Capital Expenditure Request Portal", "Custom internal workflow for IT & facilities purchases", "PUBLISHED", 2, 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{app2, orgId, "VENDOR-ONBOARD", "Supplier Compliance Onboarding", "Collect W9/GST & ISO certifications from new vendors", "PUBLISHED", 1, 0L, ts, ts, "seeder", "seeder"}
                    ));
            UUID form1 = UUID.randomUUID();
            UUID form2 = UUID.randomUUID();
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crt_forms (id, organization_id, app_id, form_code, name, schema_json, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(
                            new Object[]{form1, orgId, app1, "CAPEX-REQ", "Hardware Request Form", "[{\"name\":\"item\",\"type\":\"text\"},{\"name\":\"amount\",\"type\":\"number\"}]", 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{form2, orgId, app2, "VENDOR-KYC", "Vendor KYB Submission", "[{\"name\":\"company\",\"type\":\"text\"},{\"name\":\"taxId\",\"type\":\"text\"}]", 0L, ts, ts, "seeder", "seeder"}
                    ));
            List<Object[]> recRows = new ArrayList<>(10);
            for (int i = 1; i <= 10; i++) {
                recRows.add(new Object[]{
                        UUID.randomUUID(), orgId, i % 2 == 0 ? form2 : form1,
                        String.format("REC-D%04d", i),
                        "{\"item\":\"Workstation Fleet Batch #" + i + "\",\"amount\":" + (1800 + i * 200) + "}",
                        "admin@zeniusone.local",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO crt_records (id, organization_id, form_id, record_number, data_json, submitted_by, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    recRows);
            counts.put("creatorApps", 2);
            counts.put("creatorRecords", recRows.size());
        }

        // 16. Workflow Automation & RPA
        if (countTable("aut_workflows", orgId) == 0) {
            List<Object[]> wfRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "Auto-Notify Slack & Mail on High-Value Deal", "DEAL_STAGE_CHANGED", "amount >= 25000", "SEND_NOTIFICATION", "{\"channel\":\"enterprise-deals\"}", true, 14, ts, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Escalate Urgent SLA Ticket at 80% Elapsed", "TICKET_CREATED", "priority == 'URGENT'", "ESCALATE_TICKET", "{\"team\":\"tier2\"}", true, 9, ts, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Create Onboarding Tasks on Candidate Hire", "CANDIDATE_HIRED", "stage == 'HIRED'", "CREATE_PROJECT_TASK", "{\"template\":\"it-onboarding\"}", true, 6, ts, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "Weekly Overdue Invoice Reminder Digest", "CRON_MONDAY_0900", "status == 'OVERDUE'", "SEND_MAIL_DIGEST", "{\"cc\":\"finance\"}", true, 22, ts, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO aut_workflows (id, organization_id, name, trigger_event, condition_expr, action_type, action_config_json, enabled, run_count, last_run_at, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    wfRows);
            counts.put("automationWorkflows", wfRows.size());
        }
        List<UUID> wfIds = queryIds("SELECT id FROM aut_workflows WHERE organization_id = ?", orgId);

        if (countTable("aut_executions", orgId) == 0 && !wfIds.isEmpty()) {
            List<Object[]> exRows = new ArrayList<>(10);
            for (int i = 1; i <= 10; i++) {
                exRows.add(new Object[]{
                        UUID.randomUUID(), orgId, wfIds.get((i - 1) % wfIds.size()),
                        "{\"source\":\"seed\",\"sequence\":" + i + "}",
                        "SUCCESS", "Completed automated workflow action #" + i + " cleanly.", 45L + (i * 8L), ts,
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO aut_executions (id, organization_id, workflow_id, trigger_payload_json, status, output_summary, duration_ms, executed_at, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    exRows);
            counts.put("automationExecutions", exRows.size());
        }

        // 17. Zenius AI Knowledge Docs, Chunks & HITL Proposals
        if (countTable("ai_knowledge_docs", orgId) == 0) {
            UUID k1 = UUID.randomUUID();
            UUID k2 = UUID.randomUUID();
            UUID k3 = UUID.randomUUID();
            jdbcTemplate.batchUpdate(
                    "INSERT INTO ai_knowledge_docs (id, organization_id, title, source_type, content_text, chunk_count, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(
                            new Object[]{k1, orgId, "Enterprise Discount & Deal Desk Playbook 2026", "PLAYBOOK", "Deals above $50,000 qualify for up to 15% multi-year commitment discount with VP Finance sign-off.", 1, 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{k2, orgId, "SOC2 Type II Security & Incident Response SOP", "POLICY", "Critical security incidents must be triaged within 15 minutes and logged in IT Security with full audit trails.", 1, 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{k3, orgId, "Support Escalation & Customer Refund Guidelines", "SUPPORT_SOP", "Enterprise customers with Urgent SLA breaches qualify for priority engineering assignment and service credits.", 1, 0L, ts, ts, "seeder", "seeder"}
                    ));
            jdbcTemplate.batchUpdate(
                    "INSERT INTO ai_knowledge_chunks (id, organization_id, document_id, chunk_index, chunk_text, embedding_json, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(
                            new Object[]{UUID.randomUUID(), orgId, k1, 0, "Deals above $50,000 qualify for up to 15% multi-year commitment discount with VP Finance sign-off.", "[0.21,0.44,0.18,0.67]", 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{UUID.randomUUID(), orgId, k2, 0, "Critical security incidents must be triaged within 15 minutes and logged in IT Security.", "[0.52,0.19,0.74,0.33]", 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{UUID.randomUUID(), orgId, k3, 0, "Enterprise customers with Urgent SLA breaches qualify for priority engineering assignment.", "[0.38,0.62,0.11,0.49]", 0L, ts, ts, "seeder", "seeder"}
                    ));
            jdbcTemplate.batchUpdate(
                    "INSERT INTO ai_action_proposals (id, organization_id, conversation_id, agent_code, action_type, summary, payload_json, status, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(
                            new Object[]{UUID.randomUUID(), orgId, null, "SALES_COACH", "FOLLOWUP_DEAL", "Schedule executive sponsor check-in for top 3 Proposal-stage deals ($180k total)", "{\"dealCount\":3}", "PENDING", 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{UUID.randomUUID(), orgId, null, "FINANCE_AUDITOR", "SEND_REMINDER", "Dispatch automated AR reminder batch for overdue invoices over 15 days", "{\"invoiceBatch\":\"OVERDUE\"}", "PENDING", 0L, ts, ts, "seeder", "seeder"},
                            new Object[]{UUID.randomUUID(), orgId, null, "SUPPORT_TRIAGE", "ESCALATE_TICKET", "Route 2 aging High-priority support tickets to Senior Cloud Specialist queue", "{\"priority\":\"HIGH\"}", "PENDING", 0L, ts, ts, "seeder", "seeder"}
                    ));
            counts.put("aiKnowledgeDocs", 3);
            counts.put("aiProposals", 3);
        }

        // 18. IT Security Assets, Incidents & Policy
        if (countTable("sec_it_assets", orgId) == 0) {
            List<Object[]> assetRows = new ArrayList<>(20);
            for (int i = 1; i <= 20; i++) {
                assetRows.add(new Object[]{
                        UUID.randomUUID(), orgId,
                        String.format("AST-D%03d", i),
                        "Corporate Endpoint / Node #" + i,
                        ASSET_TYPES[i % ASSET_TYPES.length],
                        FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[i % LAST_NAMES.length],
                        i % 2 == 0 ? "Ubuntu 24.04 LTS" : "macOS 15",
                        "ACTIVE",
                        "COMPLIANT",
                        0L, ts, ts, "seeder", "seeder"
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sec_it_assets (id, organization_id, asset_tag, name, asset_type, owner_name, os_version, status, compliance_state, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    assetRows);
            counts.put("itAssets", assetRows.size());
        }

        if (countTable("sec_incidents", orgId) == 0) {
            List<Object[]> incRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, "INC-D001", "Unrecognized API Key Spike in EU Region", "HIGH", "OPEN", "soc@zeniusone.local", null, null, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "INC-D002", "Repeated Failed SSO Login Attempts", "MEDIUM", "INVESTIGATING", "iam@zeniusone.local", null, null, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, "INC-D003", "Endpoint Encryption Verification Check", "LOW", "RESOLVED", "it-ops@zeniusone.local", "BitLocker/FileVault verified across laptop group.", ts, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sec_incidents (id, organization_id, incident_number, title, severity, status, reporter_email, resolution_notes, resolved_at, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    incRows);
            counts.put("securityIncidents", incRows.size());
        }

        if (countTable("sec_policies", orgId) == 0) {
            jdbcTemplate.update(
                    "INSERT INTO sec_policies (id, organization_id, min_password_length, require_mfa, session_timeout_minutes, allowed_ip_cidr, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(), orgId, 12, true, 480, "0.0.0.0/0", 0L, ts, ts, "seeder", "seeder");
            counts.put("securityPolicies", 1);
        }

        // 19. Centralized Notifications Feed (registration creates 1 welcome notification, so seed <= 1)
        if (countTable("notifications", orgId) <= 1 && ownerUserId != null) {
            List<Object[]> notifRows = List.of(
                    new Object[]{UUID.randomUUID(), orgId, ownerUserId, "Welcome to Zenius One Suite", "Your multi-service enterprise workspace and demo dataset are active across all 20 modules.", "SYSTEM", "/", null, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, ownerUserId, "Unread Executive Mail: Q4 Budget Approval", "cfo@acmeglobal.example.com sent a high-priority approval message to your inbox.", "MAIL", "/collaboration", null, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, ownerUserId, "Urgent SLA Ticket Requires Triage", "Customer Support Case TKT-D0004 is approaching SLA threshold.", "SUPPORT", "/support", null, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, ownerUserId, "AI Sales Coach Recommendation Ready", "Review 3 high-value deals recommended for executive follow-up in Zenius AI.", "AI", "/ai", null, 0L, ts, ts, "seeder", "seeder"},
                    new Object[]{UUID.randomUUID(), orgId, ownerUserId, "Contract Signature Pending", "MSA renewal waiting on client cryptographic signature in WorkDrive.", "DOCS", "/documents", null, 0L, ts, ts, "seeder", "seeder"}
            );
            jdbcTemplate.batchUpdate(
                    "INSERT INTO notifications (id, organization_id, recipient_user_id, title, message, category, link_path, read_at, version, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    notifRows);
            counts.put("notifications", notifRows.size());
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        return new SeedResult(orgId, total == 0, total, counts);
    }

    private int countActive(String table, UUID orgId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE organization_id = ? AND deleted_at IS NULL",
                Integer.class, orgId);
        return n != null ? n : 0;
    }

    private int countTable(String table, UUID orgId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE organization_id = ?",
                Integer.class, orgId);
        return n != null ? n : 0;
    }

    private List<UUID> queryIds(String sql, UUID orgId) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> UUID.fromString(rs.getString(1)), orgId);
    }

    public record SeedResult(UUID organizationId, boolean alreadySeeded, int totalRecordsCreated, Map<String, Integer> breakdown) {}
}
