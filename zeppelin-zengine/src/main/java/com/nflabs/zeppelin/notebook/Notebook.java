package com.nflabs.zeppelin.notebook;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import com.nflabs.zeppelin.interpreter.InterpreterFactory;
import com.nflabs.zeppelin.interpreter.InterpreterSetting;
import com.nflabs.zeppelin.scheduler.Scheduler;
import com.nflabs.zeppelin.scheduler.SchedulerFactory;

/**
 * Collection of Notes.
 */
public class Notebook {
  Logger logger = LoggerFactory.getLogger(Notebook.class);
  private SchedulerFactory schedulerFactory;
  private InterpreterFactory replFactory;
  /** Keep the order. */
  Map<String, Map<String, Note>> notes = new LinkedHashMap<>();
  private ZeppelinConfiguration conf;
  private StdSchedulerFactory quertzSchedFact;
  private org.quartz.Scheduler quartzSched;
  private JobListenerFactory jobListenerFactory;

  public Notebook(ZeppelinConfiguration conf, SchedulerFactory schedulerFactory,
      InterpreterFactory replFactory, JobListenerFactory jobListenerFactory) throws IOException,
      SchedulerException {
    this.conf = conf;
    this.schedulerFactory = schedulerFactory;
    this.replFactory = replFactory;
    this.jobListenerFactory = jobListenerFactory;
    quertzSchedFact = new org.quartz.impl.StdSchedulerFactory();
    quartzSched = quertzSchedFact.getScheduler();
    quartzSched.start();
    CronJob.notebook = this;

    loadAllNotes();
  }

  /**
   * Create new note.
   *
   * @return
   * @throws IOException
   */
  public Note createNote(String principal) throws IOException {
    if (conf.getBoolean(ConfVars.ZEPPELIN_NOTEBOOK_AUTO_INTERPRETER_BINDING)) {
      return createNote(replFactory.getDefaultInterpreterSettingList(), principal);
    } else {
      return createNote(null, principal);
    }
  }

  private Map<String, Note> getUserNotes(String principal) {
    synchronized (notes) {
      Map<String, Note> userNotes = notes.get(principal);
      if (userNotes == null)
        userNotes = new HashMap<>();
      notes.put(principal, userNotes);
      return userNotes;
    }


  }

  /**
   * Create new note.
   *
   * @return
   * @throws IOException
   */
  public Note createNote(List<String> interpreterIds, String principal) throws IOException {
    NoteInterpreterLoader intpLoader = new NoteInterpreterLoader(replFactory);
    Note note = new Note(conf, intpLoader, jobListenerFactory, quartzSched, principal);
    intpLoader.setNoteId(note.id());
    synchronized (notes) {
      getUserNotes(principal).put(note.id(), note);
    }
    if (interpreterIds != null) {
      bindInterpretersToNote(note.id(), interpreterIds, principal);
    }

    return note;
  }

  public void bindInterpretersToNote(String id,
      List<String> interpreterSettingIds, String principal) throws IOException {
    Note note = getNote(id, principal);
    if (note != null) {
      note.getNoteReplLoader().setInterpreters(interpreterSettingIds);
      replFactory.putNoteInterpreterSettingBinding(id, interpreterSettingIds);
    }
  }

  public List<String> getBindedInterpreterSettingsIds(String id, String principal) {
    Note note = getNote(id, principal);
    if (note != null) {
      return note.getNoteReplLoader().getInterpreters();
    } else {
      return new LinkedList<String>();
    }
  }

  public List<InterpreterSetting> getBindedInterpreterSettings(String id, String principal) {
    Note note = getNote(id, principal);
    if (note != null) {
      return note.getNoteReplLoader().getInterpreterSettings();
    } else {
      return new LinkedList<InterpreterSetting>();
    }
  }

  public Note getNote(String id, String principal) {
    synchronized (notes) {
      return getUserNotes(principal).get(id);
    }
  }

  public void removeNote(String id, String principal) {
    Note note;
    synchronized (notes) {
      note = getUserNotes(principal).remove(id);
    }
    try {
      note.unpersist();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadAllNotes() throws IOException {
    File notebookDir = new File(conf.getNotebookDir());
    logger.info("Notebook Directory " + notebookDir.getAbsolutePath());
    File[] userdirs = notebookDir.listFiles();
    if (userdirs == null) {
      return;
    }
    for (File fuser : userdirs) {
      String principal = fuser.getName();
      boolean isHidden = principal.startsWith(".");
      if (fuser.isDirectory() && !isHidden) {
        File[] dirs = fuser.listFiles();
        if (dirs != null) {
          for (File f : dirs) {
            boolean isHiddenFile = f.getName().startsWith(".");
            if (f.isDirectory() && !isHiddenFile) {
              String schedulerName = "note_" + System.currentTimeMillis();
              Scheduler scheduler =
                      schedulerFactory.createOrGetFIFOScheduler(schedulerName);
              logger.info("Loading note from " + f.getName());
              NoteInterpreterLoader noteInterpreterLoader =
                      new NoteInterpreterLoader(replFactory);
              Note note = Note.load(f.getName(),
                      conf,
                      noteInterpreterLoader,
                      scheduler,
                      jobListenerFactory, quartzSched, fuser.getName());
              noteInterpreterLoader.setNoteId(note.id());

              synchronized (notes) {
                getUserNotes(principal).put(note.id(), note);
                refreshCron(note.id(), principal);
              }
            }
          }
        }
      }
    }
  }

  public List<Note> getAllNotes(String principal) {
    synchronized (notes) {
      List<Note> noteList = new ArrayList<>(getUserNotes(principal).values());
      logger.info("" + noteList.size());
      Collections.sort(noteList, new Comparator() {
        @Override
        public int compare(Object one, Object two) {
          Note note1 = (Note) one;
          Note note2 = (Note) two;

          String name1 = note1.id();
          if (note1.getName() != null) {
            name1 = note1.getName();
          }
          String name2 = note2.id();
          if (note2.getName() != null) {
            name2 = note2.getName();
          }
          ((Note) one).getName();
          return name1.compareTo(name2);
        }
      });
      return noteList;
    }
  }

  public JobListenerFactory getJobListenerFactory() {
    return jobListenerFactory;
  }

  public void setJobListenerFactory(JobListenerFactory jobListenerFactory) {
    this.jobListenerFactory = jobListenerFactory;
  }

  /**
   * Cron task for the note.
   *
   * @author Leemoonsoo
   *
   */
  public static class CronJob implements org.quartz.Job {
    public static Notebook notebook;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      String principal = context.getJobDetail().getJobDataMap().getString("principal");
      String noteId = context.getJobDetail().getJobDataMap().getString("noteId");
      Note note = notebook.getNote(noteId, principal);
      note.runAll();
    }
  }

  public void refreshCron(String id, String principal) {
    removeCron(id);
    synchronized (notes) {

      Note note = getUserNotes(principal).get(id);
      if (note == null) {
        return;
      }
      Map<String, Object> config = note.getConfig();
      if (config == null) {
        return;
      }

      String cronExpr = (String) note.getConfig().get("cron");
      if (cronExpr == null || cronExpr.trim().length() == 0) {
        return;
      }


      JobDetail newJob =
              JobBuilder.newJob(CronJob.class)
                        .withIdentity(id, "note")
                        .usingJobData("noteId", id)
                        .usingJobData("principal", principal)
                        .build();

      Map<String, Object> info = note.getInfo();
      info.put("cron", null);

      CronTrigger trigger = null;
      try {
        trigger =
            TriggerBuilder.newTrigger().withIdentity("trigger_" + id, "note")
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr)).forJob(id, "note")
            .build();
      } catch (Exception e) {
        logger.error("Error", e);
        info.put("cron", e.getMessage());
      }


      try {
        if (trigger != null) {
          quartzSched.scheduleJob(newJob, trigger);
        }
      } catch (SchedulerException e) {
        logger.error("Error", e);
        info.put("cron", "Scheduler Exception");
      }
    }
  }

  private void removeCron(String id) {
    try {
      quartzSched.deleteJob(new JobKey(id, "note"));
    } catch (SchedulerException e) {
      logger.error("Can't remove quertz " + id, e);
    }
  }

  public InterpreterFactory getInterpreterFactory() {
    return replFactory;
  }


}
