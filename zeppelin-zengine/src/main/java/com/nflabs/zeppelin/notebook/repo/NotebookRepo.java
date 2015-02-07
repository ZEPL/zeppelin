package com.nflabs.zeppelin.notebook.repo;

import java.io.IOException;
import java.util.List;

import com.nflabs.zeppelin.notebook.Note;
import com.nflabs.zeppelin.notebook.NoteInfo;

/**
 *
 */
public interface NotebookRepo {
  public List<NoteInfo> list() throws IOException;
  public Note get(String noteId) throws IOException;
  public void save(Note note) throws IOException;
  public void remove(String noteId) throws IOException;
}
