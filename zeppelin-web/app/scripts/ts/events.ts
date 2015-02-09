module zeppelin {

  export class ZEvent {
    op:string;
    data:any;

    toJson() {
      return {op: this.op, data: this.data};
    }
  }

  // notebook events
  export var ZNoteUpdateEventOp = 'NOTE_UPDATE';
  export class ZNoteUpdateEvent extends ZEvent {
    constructor(note: Notebook) {
      super();
      this.op = ZNoteUpdateEventOp;
      this.data = {id: note.id, name: note.name, config: note.config};
    }
  }

  export var ZNewNoteEventOp = 'NEW_NOTE';
  export class ZNewNoteEvent extends ZEvent {
    op = ZNewNoteEventOp;
  }

  export var ZListNoteEventOp = 'LIST_NOTES';
  export class ZListNoteEvent extends ZEvent {
    op = ZListNoteEventOp;
  }

  // paragraph events
  export var ZCancelParagraphEventOp = 'CANCEL_PARAGRAPH';
  export class ZCancelParagraphEvent extends ZEvent {
    constructor(paragraph: Paragraph) {
      super();
      this.op = ZCancelParagraphEventOp;
      this.data = {id: paragraph.id};
    }
  }

}
