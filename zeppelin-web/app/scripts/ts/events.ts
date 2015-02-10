module zeppelin {

  // ported from Message.java, zeppelin-server
  export class OP {
    static GET_NOTE = 'GET_NOTE';
    static NOTE = 'NOTE';
    static PARAGRAPH = 'PARAGRAPH';
    static PROGRESS = 'PROGRESS';
    static NEW_NOTE = 'NEW_NOTE';
    static DEL_NOTE = 'DEL_NOTE';
    static NOTE_UPDATE = 'NOTE_UPDATE';
    static RUN_PARAGRAPH = 'RUN_PARAGRAPH';
    static COMMIT_PARAGRAPH = 'COMMIT_PARAGRAPH';
    static CANCEL_PARAGRAPH = 'CANCEL_PARAGRAPH';
    static MOVE_PARAGRAPH = 'MOVE_PARAGRAPH';
    static INSERT_PARAGRAPH = 'INSERT_PARAGRAPH';
    static COMPLETION = 'COMPLETION';
    static COMPLETION_LIST = 'COMPLETION_LIST';
    static LIST_NOTES = 'LIST_NOTES';
    static NOTES_INFO = 'NOTES_INFO';
    static PARAGRAPH_REMOVE = 'PARAGRAPH_REMOVE';
  }

  export class ZEvent {
    op:string;
    data:any;

    toJson() {
      return {op: this.op, data: this.data};
    }
  }

  // notebook events
  export class ZNoteUpdateEvent extends ZEvent {
    constructor(note: Notebook) {
      super();
      this.op = OP.NOTE_UPDATE;
      this.data = {id: note.id, name: note.name, config: note.config};
    }
  }

  export class ZGetNoteEvent extends ZEvent {
    constructor(notebookId: string) {
      super();
      this.op = OP.GET_NOTE;
      this.data = {id: notebookId};
    }
  }

  export class ZDeleteNoteEvent extends ZEvent {
    constructor(notebook: Notebook) {
      super();
      this.op = OP.DEL_NOTE;
      this.data = {id: notebook.id};
    }
  }

  export class ZNewNoteEvent extends ZEvent {
    op = OP.NEW_NOTE;
  }

  export class ZListNotesEvent extends ZEvent {
    op = OP.LIST_NOTES;
  }

  // paragraph events
  export class ZCancelParagraphEvent extends ZEvent {
    constructor(paragraph: Paragraph) {
      super();
      this.op = OP.CANCEL_PARAGRAPH;
      this.data = {id: paragraph.id};
    }
  }

  export class ZMoveParagraphEvent extends ZEvent {
    constructor(paragraphId: string, toIndex: number) {
      super();
      this.op = OP.MOVE_PARAGRAPH;
      this.data = {id: paragraphId, index: toIndex};
    }
  }

  export class ZInsertParagraphEvent extends ZEvent {
    constructor(toIndex: number) {
      super();
      this.op = OP.INSERT_PARAGRAPH;
      this.data = {index: toIndex};
    }
  }

  export class ZRemoveParagraphEvent extends ZEvent {
    constructor(paragraph: Paragraph) {
      super();
      this.op = OP.PARAGRAPH_REMOVE;
      this.data = {id: paragraph.id};
    }
  }

  export class ZRunParagraphEvent extends ZEvent {
    constructor(paragraph: Paragraph, content: string) {
      super();
      this.op = OP.RUN_PARAGRAPH;
      this.data = {
        id: paragraph.id,
        title: paragraph.title,
        paragraph: content,
        config: paragraph.config,
        params: paragraph.settings.params
      };
    }
  }

  export class ZCodeCompletionEvent extends ZEvent {
    constructor(paragraph: Paragraph, buffer: any) {
      super();
      this.op = OP.COMPLETION;
      this.data = {
        id: paragraph.id,
        buf: buffer,
        cursor: buffer.length
      };
    }
  }

  export class ZCommitParagraphEvent extends ZEvent {
    constructor(paragraph: Paragraph, config, params) {
      super();
      this.op = OP.COMMIT_PARAGRAPH;
      this.data = {
        id: paragraph.id,
        title: paragraph.title,
        paragraph: paragraph.text,
        params: params,
        config: config
      };
    }
  }
}
