/* global $:false, jQuery:false, ace:false, confirm:false, d3:false, nv:false*/
/*jshint loopfunc: true, unused:false */
/* Copyright 2014 NFLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

/**
 * @ngdoc function
 * @name zeppelinWebApp.controller:ParagraphCtrl
 * @description
 * # ParagraphCtrl
 * Controller of the paragraph, manage everything related to the paragraph
 *
 * @author anthonycorbacho
 */

module zeppelin {

  export class Paragraph {
    id: string;
    title: string;
    text: any;
    config: ParagraphConfig;

    settings: any;

    jobName: string;
    status: string;
    dateStarted: string;
    dateCreated: string;
    dateFinished: string;

    result: ParagraphResult;
    errorMessage: any;
    aborted: any; // not using?

    constructor(p: Paragraph) {
      angular.copy(p, this);
      this.config = new ParagraphConfig(p.config);
      this.result = new ParagraphResult(p.result);
    }

    graphMode(): string {
      if (this.config.graph && this.config.graph.mode) {
        return this.config.graph.mode;
      } else {
        return GraphMode.table; // default mode
      }
    }

    executionTimeString(): string {
      var timeMs = Date.parse(this.dateFinished) - Date.parse(this.dateStarted);
      if (isNaN(timeMs)) {
        return '&nbsp;';
      }
      return 'Took ' + (timeMs/1000) + ' seconds';
    }

    base64ImageSrc(): string {
      return 'data:image/png;base64,' + this.result.msg;
    }
  }

  export class ParagraphResult {
    type: string;
    msg: string;
    columnNames: Array<any>;
    msgTable: Array<any>;

    constructor(r: ParagraphResult) {
      angular.copy(r, this);
      if (!this.type) {
        this.type = PResultType.TEXT;
      }
    }
  }

  export class ParagraphConfig {
    title: boolean;
    colWidth: number;
    graph: ParagraphGraph;
    editorHide: boolean;
    tableHide: boolean;

    initializeDefaultValues() {
      if (!this.colWidth) {
        this.colWidth = 12;
      }
      if (!this.graph) {
        this.graph = new ParagraphGraph();
      }
      if (!this.graph.mode) {
        this.graph.mode = GraphMode.table;
      }
      if (!this.graph.height) {
        this.graph.height = 300;
      }
      if (!this.graph.optionOpen) {
        this.graph.optionOpen = false;
      }
      if (!this.graph.keys) {
        this.graph.keys = [];
      }
      if (!this.graph.values) {
        this.graph.values = [];
      }
      if (!this.graph.groups) {
        this.graph.groups = [];
      }
    }

    constructor(c: ParagraphConfig) {
      angular.copy(c, this);
      this.initializeDefaultValues();
    }
  }

  export class ParagraphGraph {
    mode: string;
    height: number;
    optionOpen: boolean;
    keys: any;
    values: any;
    groups: any;

    constructor() {}
  }

  export class GraphMode {
    static table = 'table';
    static multiBarChart = 'multiBarChart';
    static pieChart = 'pieChart';
    static stackedAreaChart = 'stackedAreaChart';
    static lineChart = 'lineChart';
  }

  export class PResultType {
    static TEXT = 'TEXT';
    static TABLE = 'TABLE';
    static HTML = 'HTML';
    static IMG = 'IMG';
  }

  export class PStatus {
    static RUNNING = 'RUNNING';
    static PENDING = 'PENDING';
  }

  export interface IParagraphCtrlScope extends ng.IScope {
    init: (paragraph: Paragraph) => void;
    paragraph: Paragraph;

    editingText: string;
    colWidthOption: Array<number>;

    // status flags
    asIframe: boolean;
    currentProgress: number;
    isRunningOrPending: () => boolean;
    showTitleEditor: boolean;

    getIframeDimensions: () => number;

    loadForm: (formulaire: any, params: any) => void;

    // run buttons
    runParagraph: () => void;
    cancelParagraph: () => void;
    toggleEditor: () => void;
    toggleOutput: () => void;

    // setting buttons
    columnWidthClass: (n: number) => string;
    moveUp: () => void;
    moveDown: () => void;
    insertNew: () => void;
    goToSingleParagraph: () => void;
    removeParagraph: () => void;

    commitParagraph: () => void;

    // chart
    toggleGraphOption: () => void;
    isGraphMode: (graphName: string) => boolean;

    setGraphHeight: () => void;

    // to be refactored
    lastData: any; // storing only settings and config? what use?
    $watch: any; // treat in proper way
    $digest: any; // treat in proper way
  }

  angular.module('zeppelinWebApp').controller('ParagraphCtrl', function(
    $scope: IParagraphCtrlScope,
    $rootScope: IZeppelinRootScope,
    $route: any,
    $window: ng.IWindowService,
    $element: ng.IRootElementService,
    $routeParams: any,
    $location: any) {

    // Controller init
    $scope.init = function(newParagraph: Paragraph) {
      $scope.paragraph = new Paragraph(newParagraph);

      $scope.colWidthOption = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
      $scope.showTitleEditor = false;
      $scope.currentProgress = 0;
      $scope.lastData = {};
    };

    $scope.getIframeDimensions = function () {
      if ($scope.asIframe) {
        var paragraphid = '#' + $routeParams.paragraphId + '_container';
        var height = $(paragraphid).height();
        return height;
      }
      return 0;
    };

    $scope.$watch($scope.getIframeDimensions, function (newValue, oldValue) {
      if ($scope.asIframe && newValue) {
        var message = {height: newValue, url: $location.$$absUrl};
        $window.parent.postMessage(angular.toJson(message), '*');
      }
    });

    // TODO: this may have impact on performance when there are many paragraphs in a note.
    $scope.$on('updateParagraph', function(event, data) {
      var updatedParagraph = new Paragraph(data.paragraph);

      if (updatedParagraph.id !== $scope.paragraph.id) return;
      if (updatedParagraph.dateCreated !== $scope.paragraph.dateCreated ||
          updatedParagraph.dateFinished !== $scope.paragraph.dateFinished ||
          updatedParagraph.dateStarted !== $scope.paragraph.dateStarted ||
          updatedParagraph.status !== $scope.paragraph.status ||
          updatedParagraph.jobName !== $scope.paragraph.jobName ||
          updatedParagraph.title !== $scope.paragraph.title ||
          updatedParagraph.errorMessage !== $scope.paragraph.errorMessage ||
          !angular.equals(updatedParagraph.settings, $scope.lastData.settings) ||
          !angular.equals(updatedParagraph.config, $scope.lastData.config)) {

        // store original data for comparison
        $scope.lastData.settings = angular.copy(updatedParagraph.settings);
        $scope.lastData.config = angular.copy(updatedParagraph.config);

        //console.log('updateParagraph oldData %o, newData %o. type %o -> %o, mode %o -> %o', $scope.paragraph, data, oldType, newType, oldGraphMode, newGraphMode);

        var updatedText = updatedParagraph.text;
        if ($scope.paragraph.text !== updatedText) {
          if ($scope.editingText) { // check if editor has local update
            $scope.paragraph.text = $scope.editingText;
            if ($scope.editingText === updatedText) {  // when local update is the same from remote, clear local update
              $scope.editingText = undefined;
            }
          } else {
            $scope.paragraph.text = updatedText;
          }
        }

        /** push the rest */
        $scope.paragraph.aborted = updatedParagraph.aborted;
        $scope.paragraph.dateCreated = updatedParagraph.dateCreated;
        $scope.paragraph.dateFinished = updatedParagraph.dateFinished;
        $scope.paragraph.dateStarted = updatedParagraph.dateStarted;
        $scope.paragraph.errorMessage = updatedParagraph.errorMessage;
        $scope.paragraph.jobName = updatedParagraph.jobName;
        $scope.paragraph.title = updatedParagraph.title;
        $scope.paragraph.status = updatedParagraph.status;
        $scope.paragraph.result = updatedParagraph.result;
        $scope.paragraph.settings = updatedParagraph.settings;
        $scope.paragraph.config = updatedParagraph.config;

        if ($scope.asIframe) {
          $scope.paragraph.config.editorHide = true;
          $scope.paragraph.config.tableHide = false;
        }
      }
    });

    $scope.isRunningOrPending = function() {
      return $scope.paragraph.status === PStatus.RUNNING || $scope.paragraph.status === PStatus.PENDING;
    };

    $scope.cancelParagraph = function() {
      $rootScope.sendEventToServer(new ZCancelParagraphEvent($scope.paragraph));
    };

    $scope.runParagraph = function() {
      $rootScope.sendEventToServer(new ZRunParagraphEvent($scope.paragraph));
    };

    $scope.moveUp = function() {
      $scope.$emit('moveParagraphUp', $scope.paragraph.id);
    };

    $scope.moveDown = function() {
      $scope.$emit('moveParagraphDown', $scope.paragraph.id);
    };

    $scope.insertNew = function() {
      $scope.$emit('insertParagraph', $scope.paragraph.id);
    };

    $scope.removeParagraph = function() {
      var result = confirm('Do you want to delete this paragraph?');
      if (result) {
        console.log('Remove paragraph');
        var paragraphData = new ZRemoveParagraphEvent($scope.paragraph);
        $rootScope.sendEventToServer(paragraphData);
      }
    };

    $scope.toggleEditor = function() {
      $scope.paragraph.config.editorHide = !$scope.paragraph.config.editorHide;
      $scope.commitParagraph();
    };

    $scope.columnWidthClass = function(n) {
      if ($scope.asIframe || !n) {
        return 'col-md-12';
      } else {
        return 'col-md-' + n;
      }
    };

    $scope.toggleGraphOption = function() {
      $scope.paragraph.config.graph.optionOpen = !$scope.paragraph.config.graph.optionOpen;
      $scope.commitParagraph();
    };

    $scope.toggleOutput = function() {
      $scope.paragraph.config.tableHide = !$scope.paragraph.config.tableHide;
      $scope.commitParagraph();
    };

    $scope.loadForm = function(formulaire, params) {
      var value = formulaire.defaultValue;
      if (params[formulaire.name]) {
        value = params[formulaire.name];
      }

      if (value === '') {
        value = formulaire.options[0].value;
      }

      $scope.paragraph.settings.params[formulaire.name] = value;
    };

    $scope.$on('updateProgress', function(event, data) {
      if (data.id === $scope.paragraph.id) {
        $scope.currentProgress = data.progress;
      }
    });

    $scope.$on('focusParagraph', function(event, paragraphId) {
      if ($scope.paragraph.id === paragraphId) {
        $('body').scrollTo('#' + paragraphId + '_editor', 300, {offset:-60});
      }
    });

    $scope.$on('runParagraph', function(event) {
      $scope.runParagraph();
    });

    $scope.$on('openEditor', function(event) {
      $scope.paragraph.config.editorHide = false;
      $scope.commitParagraph();
    });

    $scope.$on('closeEditor', function(event) {
      $scope.paragraph.config.editorHide = true;
      $scope.commitParagraph();
    });

    $scope.$on('openTable', function(event) {
      $scope.paragraph.config.tableHide = false;
      $scope.commitParagraph();
    });

    $scope.$on('closeTable', function(event) {
      $scope.paragraph.config.tableHide = true;
      $scope.commitParagraph();
    });

    $scope.commitParagraph = function() {
      var parapgraphData = new ZCommitParagraphEvent($scope.paragraph);
      $rootScope.sendEventToServer(parapgraphData);
    };

    $scope.isGraphMode = function(graphName) {
      return $scope.paragraph &&
        $scope.paragraph.result.type === PResultType.TABLE &&
        $scope.paragraph.graphMode() === graphName;
    };

    $scope.setGraphHeight = function() {
      var height = $('#p' + $scope.paragraph.id + '_graph').height();
      $scope.paragraph.config.graph.height = height;
      $scope.commitParagraph();
    };

    $scope.goToSingleParagraph = function () {
      var noteId = $route.current.pathParams.noteId;
      var redirectToUrl = location.protocol + '//' + location.host + '/#/notebook/' + noteId + '/paragraph/' + $scope.paragraph.id + '?asIframe';
      $window.open(redirectToUrl);
    };
  });
}
