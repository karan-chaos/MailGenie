package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Preloads the database with a set of default email snippets covering
 * common business communication patterns: acknowledgements, follow-ups,
 * meeting coordination, and professional courtesies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SnippetInitializer implements CommandLineRunner {

    private final EmailSnippetRepository snippetRepository;

    @Override
    public void run(String... args) {
        if (snippetRepository.count() > 0) {
            log.info("Snippets already initialized, skipping preload.");
            return;
        }

        List<EmailSnippet> defaults = List.of(
                EmailSnippet.builder()
                        .title("Quick Acknowledgement")
                        .body("Hi {name},\n\nThank you for your email. I have received it and will review the details shortly.\n\nI will get back to you with a more detailed response by {date}.\n\nBest regards,\n{sender}")
                        .category("Acknowledgement")
                        .shortcut("/ack")
                        .requiredVariables("name,date,sender")
                        .defaultVariableValues("name=there;date=tomorrow;sender=[Your Name]")
                        .tone("professional")
                        .pinned(true)
                        .shared(true)
                        .build(),

                EmailSnippet.builder()
                        .title("Meeting Request")
                        .body("Hi {name},\n\nI would like to schedule a meeting to discuss {topic}. Would you be available on {date} at {time}?\n\nPlease let me know if this works for you, or suggest an alternative time.\n\nBest regards,\n{sender}")
                        .category("Meeting")
                        .shortcut("/meet")
                        .requiredVariables("name,topic,date,time,sender")
                        .defaultVariableValues("name=there;topic=the project update;date=next Monday;time=2:00 PM;sender=[Your Name]")
                        .tone("professional")
                        .pinned(true)
                        .shared(true)
                        .build(),

                EmailSnippet.builder()
                        .title("Follow-Up Reminder")
                        .body("Hi {name},\n\nI hope this message finds you well. I wanted to follow up on my previous email regarding {topic}.\n\nI understand you may have a busy schedule, but I would appreciate it if you could share your thoughts when you get a chance.\n\nLooking forward to hearing from you.\n\nBest regards,\n{sender}")
                        .category("Follow-up")
                        .shortcut("/fu")
                        .requiredVariables("name,topic,sender")
                        .defaultVariableValues("name=there;topic=our discussion;sender=[Your Name]")
                        .tone("friendly")
                        .pinned(false)
                        .shared(true)
                        .build(),

                EmailSnippet.builder()
                        .title("Thank You Note")
                        .body("Hi {name},\n\nI wanted to take a moment to thank you for {reason}. Your support and assistance have been invaluable, and I truly appreciate the time you dedicated to helping with this.\n\nPlease do not hesitate to reach out if there is anything I can do in return.\n\nWarm regards,\n{sender}")
                        .category("Courtesy")
                        .shortcut("/thx")
                        .requiredVariables("name,reason,sender")
                        .defaultVariableValues("name=there;reason=your help with this matter;sender=[Your Name]")
                        .tone("friendly")
                        .pinned(false)
                        .shared(true)
                        .build(),

                EmailSnippet.builder()
                        .title("Status Update Request")
                        .body("Hi {name},\n\nI am reaching out to request an update on {project}. Could you please share the current status and any blockers that need to be addressed?\n\nA brief summary would be very helpful for our upcoming review on {date}.\n\nThank you,\n{sender}")
                        .category("Request")
                        .shortcut("/status")
                        .requiredVariables("name,project,date,sender")
                        .defaultVariableValues("name=team;project=the project;date=next week;sender=[Your Name]")
                        .tone("professional")
                        .pinned(false)
                        .shared(true)
                        .build(),

                EmailSnippet.builder()
                        .title("Introduction / Cold Outreach")
                        .body("Hi {name},\n\nMy name is {sender} and I am reaching out regarding {topic}. I came across {context} and believe there could be a great opportunity for collaboration.\n\nWould you be open to a brief call this week to discuss further?\n\nLooking forward to connecting.\n\nBest regards,\n{sender}")
                        .category("Outreach")
                        .shortcut("/intro")
                        .requiredVariables("name,topic,context,sender")
                        .defaultVariableValues("name=there;topic=a potential partnership;context=your work at {company};sender=[Your Name]")
                        .tone("persuasive")
                        .pinned(false)
                        .shared(true)
                        .build(),

                EmailSnippet.builder()
                        .title("Delegation / Assignment")
                        .body("Hi {name},\n\nI would like to assign you the task of {task}. Please review the requirements below:\n\n- Deadline: {deadline}\n- Priority: {priority}\n- Stakeholders: {stakeholders}\n\nFeel free to reach out if you have any questions or need additional resources.\n\nBest regards,\n{sender}")
                        .category("Internal")
                        .shortcut("/assign")
                        .requiredVariables("name,task,deadline,priority,stakeholders,sender")
                        .defaultVariableValues("name=team member;task=the quarterly report;deadline=end of month;priority=High;stakeholders=Management;sender=[Your Name]")
                        .tone("professional")
                        .pinned(false)
                        .shared(false)
                        .build()
        );

        snippetRepository.saveAll(defaults);
        log.info("Preloaded {} default email snippets.", defaults.size());
    }
}
