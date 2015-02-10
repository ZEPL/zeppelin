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
    config: ParagraphConfig;
    settings: any;

    title: string;
    jobName: string;
    status: string;
    dateStarted: string;
    dateCreated: string;
    dateFinished: string;

    text: any;
    result: any;
    errorMessage: any;
    aborted: any; // not using?

    constructor(p: Paragraph) {
      angular.copy(p, this);
      this.config = new ParagraphConfig(p.config);
    }

    graphMode() {
      if (this.config.graph && this.config.graph.mode) {
        return this.config.graph.mode;
      } else {
        return GraphMode.table; // default mode
      }
    }

    resultType() {
      if (this.result && this.result.type) {
        return this.result.type;
      } else {
        return PResultType.TEXT;
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
      this.title = c.title;
      this.colWidth = c.colWidth;
      this.graph = c.graph;
      this.editorHide = c.editorHide;
      this.tableHide = c.tableHide;
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

  class GraphMode {
    static table = 'table';
    static multiBarChart = 'multiBarChart';
    static pieChart = 'pieChart';
    static stackedAreaChart = 'stackedAreaChart';
    static lineChart = 'lineChart';
  }

  class PResultType {
    static TEXT = 'TEXT';
    static TABLE = 'TABLE';
    static HTML = 'HTML';
    static IMG = 'IMG';
  }

  export interface IParagraphCtrlScope extends ng.IScope {
    init: (paragraph: Paragraph) => void;
    paragraph: Paragraph;
    chart: any;
    asIframe: boolean;
    currentProgress: number;

    editingText: string;

    isRunning: () => boolean;

    colWidthOption: Array<number>;
    showTitleEditor: boolean;

    loadTableData: (result: any) => void;
    setGraphMode: (type: string, emit: boolean, refresh: boolean) => void;
    renderHtml: () => void;
    getIframeDimensions: () => number;
    cancelParagraph: () => void;
    runParagraph: () => void;
    loadForm: (formulaire: any, params: any) => void;

    moveUp: () => void;
    moveDown: () => void;
    insertNew: () => void;
    removeParagraph: () => void;
    toggleEditor: () => void;
    closeEditor: () => void;
    openEditor: () => void;
    closeTable: () => void;
    openTable: () => void;
    showTitle: () => void;
    hideTitle: () => void;
    setTitle: () => void;
    columnWidthClass: (n: number) => string;
    changeColWidth: () => void;
    toggleGraphOption: () => void;
    toggleOutput: () => void;

    getExecutionTime: () => string;
    getBase64ImageSrc: (data: any) => string;

    isGraphMode: (graphName: string) => boolean;
    onGraphOptionChange: () => void;
    removeGraphOptionKeys: (idx: number) => void;
    removeGraphOptionValues: (idx: number) => void;
    removeGraphOptionGroups: (idx: number) => void;
    setGraphOptionValueAggr: (idx: number, aggr: any) => void;
    setGraphHeight: () => void;

    goToSingleParagraph: () => void;

    // to be refactored
    lastData: any; // storing only settings and config?
    $watch: any; // treat in proper way
    $digest: any; // treat in proper way
    d3: any; //
  }

  angular.module('zeppelinWebApp').controller('ParagraphCtrl', function(
    $scope: IParagraphCtrlScope,
    $rootScope: IZeppelinRootScope,
    $route: any,
    $window: ng.IWindowService,
    $element: ng.IRootElementService,
    $routeParams: any,
    $location: any,
    $timeout: ng.ITimeoutService) {

    // Controller init
    $scope.init = function(newParagraph: Paragraph) {
      $scope.paragraph = new Paragraph(newParagraph);

      $scope.colWidthOption = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
      $scope.showTitleEditor = false;
      $scope.currentProgress = 0;
      $scope.lastData = {};
      $scope.chart = {};

      if ($scope.paragraph.resultType() === PResultType.TABLE) {
        $scope.lastData.settings = angular.copy($scope.paragraph.settings);
        $scope.lastData.config = angular.copy($scope.paragraph.config);
        $scope.loadTableData($scope.paragraph.result);
        $scope.setGraphMode($scope.paragraph.graphMode(), false, false);
      } else if ($scope.paragraph.resultType() === PResultType.HTML) {
        $scope.renderHtml();
      }
    };

    $scope.renderHtml = function() {
      var retryRenderer = function() {
        if ($('#p' + $scope.paragraph.id + '_html').length) {
          try {
            $('#p' + $scope.paragraph.id + '_html').html($scope.paragraph.result.msg);
          } catch(err) {
            console.log('HTML rendering error %o', err);
          }
        } else {
          $timeout(retryRenderer,10);
        }
      };
      $timeout(retryRenderer);
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

        var oldType = $scope.paragraph.resultType();
        var newType = updatedParagraph.resultType();
        var oldGraphMode = $scope.paragraph.graphMode();
        var newGraphMode = updatedParagraph.graphMode();
        var resultRefreshed = (updatedParagraph.dateFinished !== $scope.paragraph.dateFinished);

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

        if (!$scope.asIframe) {
          $scope.paragraph.config = updatedParagraph.config;
        } else {
          updatedParagraph.config.editorHide = true;
          updatedParagraph.config.tableHide = false;
          $scope.paragraph.config = updatedParagraph.config;
        }

        if (newType === PResultType.TABLE) {
          $scope.loadTableData($scope.paragraph.result);
          if (oldType !== PResultType.TABLE || resultRefreshed) {
            clearUnknownColsFromGraphOption();
            selectDefaultColsForGraphOption();
          }
          /** User changed the chart type? */
          if (oldGraphMode !== newGraphMode) {
            $scope.setGraphMode(newGraphMode, false, false);
          } else {
            $scope.setGraphMode(newGraphMode, false, true);
          }
        } else if (newType === PResultType.HTML) {
          $scope.renderHtml();
        }
      }
    });

    $scope.isRunning = function() {
      if ($scope.paragraph.status === 'RUNNING' || $scope.paragraph.status === 'PENDING') {
        return true;
      } else {
        return false;
      }
    };

    $scope.cancelParagraph = function() {
      console.log('Cancel %o', $scope.paragraph.id);
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
      if ($scope.paragraph.config.editorHide) {
        $scope.openEditor();
      } else {
        $scope.closeEditor();
      }
    };

    $scope.closeEditor = function() {
      console.log('close the note');

      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.editorHide = true;

      commitParagraph(newConfig, newParams);
    };

    $scope.openEditor = function() {
      console.log('open the note');

      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.editorHide = false;

      commitParagraph(newConfig, newParams);
    };

    $scope.closeTable = function() {
      console.log('close the output');

      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.tableHide = true;

      commitParagraph(newConfig, newParams);
    };

    $scope.openTable = function() {
      console.log('open the output');

      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.tableHide = false;

      commitParagraph(newConfig, newParams);
    };

    $scope.showTitle = function() {
      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.title = true;

      commitParagraph(newConfig, newParams);
    };

    $scope.hideTitle = function() {
      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.title = false;

      commitParagraph(newConfig, newParams);
    };

    $scope.setTitle = function() {
      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);
      commitParagraph(newConfig, newParams);
    };

    $scope.columnWidthClass = function(n) {
      if ($scope.asIframe || !n) {
        return 'col-md-12';
      } else {
        return 'col-md-' + n;
      }
    };

    $scope.changeColWidth = function() {

      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);

      commitParagraph(newConfig, newParams);
    };

    $scope.toggleGraphOption = function() {
      var newConfig = angular.copy($scope.paragraph.config);
      if (newConfig.graph.optionOpen) {
        newConfig.graph.optionOpen = false;
      } else {
        newConfig.graph.optionOpen = true;
      }
      var newParams = angular.copy($scope.paragraph.settings.params);

      commitParagraph(newConfig, newParams);
    };

    $scope.toggleOutput = function() {
      var newConfig = angular.copy($scope.paragraph.config);
      newConfig.tableHide = !newConfig.tableHide;
      var newParams = angular.copy($scope.paragraph.settings.params);

      commitParagraph(newConfig, newParams);
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

    $scope.getExecutionTime = function() {
      var pdata = $scope.paragraph;
      var timeMs = Date.parse(pdata.dateFinished) - Date.parse(pdata.dateStarted);
      if (isNaN(timeMs)) {
        return '&nbsp;';
      }
      return 'Took ' + (timeMs/1000) + ' seconds';
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
      $scope.openEditor();
    });

    $scope.$on('closeEditor', function(event) {
      $scope.closeEditor();
    });

    $scope.$on('openTable', function(event) {
      $scope.openTable();
    });

    $scope.$on('closeTable', function(event) {
      $scope.closeTable();
    });

    $scope.getBase64ImageSrc = function(base64Data) {
      return 'data:image/png;base64,' + base64Data;
    };

    $scope.loadTableData = function(result) {
      if (!result) {
        return;
      }
      if (result.type === PResultType.TABLE) {
        var columnNames = [];
        var rows = [];
        var array = [];
        var textRows = result.msg.split('\n');
        result.comment = '';
        var comment = false;

        for (var i = 0; i < textRows.length; i++) {
          var textRow = textRows[i];
          if (comment) {
            result.comment += textRow;
            continue;
          }

          if (textRow === '') {
            if (rows.length>0) {
              comment = true;
            }
            continue;
          }
          var textCols = textRow.split('\t');
          var cols = [];
          var cols2 = [];
          for (var j = 0; j < textCols.length; j++) {
            var col = textCols[j];
            if (i === 0) {
              columnNames.push({name:col, index:j, aggr:'sum'});
            } else {
              cols.push(col);
              cols2.push({key: (columnNames[i]) ? columnNames[i].name: undefined, value: col});
            }
          }
          if (i !== 0) {
            rows.push(cols);
            array.push(cols2);
          }
        }
        result.msgTable = array;
        result.columnNames = columnNames;
        result.rows = rows;
      }
    };

    $scope.setGraphMode = function(type, emit, refresh) {
      if (emit) {
        setNewMode(type);
      } else {
        clearUnknownColsFromGraphOption();
        // set graph height
        var height = $scope.paragraph.config.graph.height;
        $('#p' + $scope.paragraph.id + '_graph').height(height);

        if (!type || type === GraphMode.table) {
          setTable($scope.paragraph.result, refresh);
        }
        else {
          setD3Chart(type, $scope.paragraph.result, refresh);
        }
      }
    };

    var setNewMode = function(newMode) {
      var newConfig = angular.copy($scope.paragraph.config);
      var newParams = angular.copy($scope.paragraph.settings.params);

      // graph options
      newConfig.graph.mode = newMode;

      commitParagraph(newConfig, newParams);
    };

    var commitParagraph = function(config, params) {
      var parapgraphData = new ZCommitParagraphEvent($scope.paragraph, config, params);
      $rootScope.sendEventToServer(parapgraphData);
    };

    var setTable = function(data, refresh) {
      var getTableContentFormat = function(d) {
        if (isNaN(d)) {
          if (d.length > '%html'.length && '%html ' === d.substring(0, '%html '.length)) {
            return 'html';
          } else {
            return '';
          }
        } else {
          return '';
        }
      };

      var formatTableContent = function(d) {
        if (isNaN(d)) {
          var f = getTableContentFormat(d);
          if (f !== '') {
            return d.substring(f.length + 2);
          } else {
            return d;
          }
        } else {
          var dStr = d.toString();
          var splitted = dStr.split('.');
          var formatted = splitted[0].replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
          if (splitted.length>1) {
            formatted += '.' + splitted[1];
          }
          return formatted;
        }
      };

      var renderTable = function() {
        var html = '';
        html += '<table class=\'table table-hover table-condensed\'>';
        html += '  <thead>';
        html += '    <tr style=\'background-color: #F6F6F6; font-weight: bold;\'>';
        for (var c in $scope.paragraph.result.columnNames) {
          html += '<th>' + $scope.paragraph.result.columnNames[c].name + '</th>';
        }
        html += '    </tr>';
        html += '  </thead>';

        for (var r in $scope.paragraph.result.msgTable) {
          var row = $scope.paragraph.result.msgTable[r];
          html += '    <tr>';
          for (var index in row) {
            var v = row[index].value;
            if (getTableContentFormat(v) !== 'html') {
              v = v.replace(/[\u00A0-\u9999<>\&]/gim, function(i) {
                  return '&#' + i.charCodeAt(0) + ';';
              });
            }
            html += '      <td>' + formatTableContent(v) + '</td>';
          }
          html += '    </tr>';
        }

        html += '</table>';

        $('#p' + $scope.paragraph.id + '_table').html(html);
        $('#p' + $scope.paragraph.id + '_table').perfectScrollbar();

        // set table height
        var height = $scope.paragraph.config.graph.height;
        $('#p' + $scope.paragraph.id + '_table').height(height);
      };

      var retryRenderer = function() {
        if ($('#p' + $scope.paragraph.id + '_table').length) {
          try {
            renderTable();
          } catch(err) {
            console.log('Chart drawing error %o', err);
          }
        } else {
          $timeout(retryRenderer,10);
        }
      };
      $timeout(retryRenderer);
    };

    var setD3Chart = function(type, data, refresh) {
      if (!$scope.chart[type]) {
        var chart = nv.models[type]();
        $scope.chart[type] = chart;
      }

      var p = pivot(data);

      var xColIndexes = $scope.paragraph.config.graph.keys;
      var yColIndexes = $scope.paragraph.config.graph.values;

      var d3g = [];
      // select yColumns.

      if (type === GraphMode.pieChart) {
        var d = pivotDataToD3ChartFormat(p, true).d3g;

        $scope.chart[type].x(function(d) { return d.label;})
                          .y(function(d) { return d.value;});

        if ( d.length > 0 ) {
          for ( var i=0; i<d[0].values.length ; i++) {
            var e = d[0].values[i];
            d3g.push({
              label : e.x,
              value : e.y
            });
          }
        }
      } else if (type === GraphMode.multiBarChart) {
        d3g = pivotDataToD3ChartFormat(p, true, false, type).d3g;
        $scope.chart[type].yAxis.axisLabelDistance(50);
      } else {
        var pivotdata = pivotDataToD3ChartFormat(p, false, true);
        var xLabels = pivotdata.xLabels;
        d3g = pivotdata.d3g;
        $scope.chart[type].xAxis.tickFormat(function(d) {
          if (xLabels[d] && (isNaN(parseFloat(xLabels[d])) || !isFinite(xLabels[d]))) { // to handle string type xlabel
            return xLabels[d];
          } else {
            return d;
          }
        });
        $scope.chart[type].yAxis.axisLabelDistance(50);
        $scope.chart[type].useInteractiveGuideline(true); // for better UX and performance issue. (https://github.com/novus/nvd3/issues/691)
        $scope.chart[type].forceY([0]); // force y-axis minimum to 0 for line chart.
      }

      var renderChart = function() {
        if (!refresh) {
          // TODO force destroy previous chart
        }

        var height = $scope.paragraph.config.graph.height;

        var animationDuration = 300;
        var numberOfDataThreshold = 150;
        // turn off animation when dataset is too large. (for performance issue)
        // still, since dataset is large, the chart content sequentially appears like animated.
        try {
          if (d3g[0].values.length > numberOfDataThreshold) {
            animationDuration = 0;
          }
        } catch(ignoreErr) {
        }

        var chartEl = d3.select('#p' + $scope.paragraph.id + '_' + type + ' svg')
            .attr('height', $scope.paragraph.config.graph.height)
            .datum(d3g)
            .transition()
            .duration(animationDuration)
            .call($scope.chart[type]);
        d3.select('#p' + $scope.paragraph.id + '_' + type + ' svg').style('height', height + 'px');
        nv.utils.windowResize($scope.chart[type].update);
      };

      var retryRenderer = function() {
        if ($('#p' + $scope.paragraph.id + '_' + type + ' svg').length !== 0) {
          try {
            renderChart();
          } catch(err) {
            console.log('Chart drawing error %o', err);
          }
        } else {
          $timeout(retryRenderer,10);
        }
      };
      $timeout(retryRenderer);
    };

    var setPieChart = function(data, refresh) {
      var xColIndex = 0;
      var yColIndexes = [];
      var d3g = [];

      // select yColumns.
      for (var colIndex = 0; colIndex < data.columnNames.length; colIndex++) {
        if (colIndex !== xColIndex) {
          yColIndexes.push(colIndex);
        }
      }

      for (var rowIndex = 0; rowIndex < data.rows.length; rowIndex++) {
        var row = data.rows[rowIndex];
        var xVar = row[xColIndex];
        var yVar = row[yColIndexes[0]];

        d3g.push({
            label: isNaN(xVar) ? xVar : parseFloat(xVar),
            value: parseFloat(yVar)
        });
      }

      if ($scope.d3.pieChart.data === null || !refresh) {
        $scope.d3.pieChart.data = d3g;
        $scope.d3.pieChart.options.chart.height = $scope.paragraph.config.graph.height;

        if ($scope.d3.pieChart.api) {
          $scope.d3.pieChart.api.updateWithOptions($scope.d3.pieChart.options);
        }
      } else {
        if ($scope.d3.pieChart.api) {
          $scope.d3.pieChart.api.updateWithData(d3g);
        }
      }
    };

    $scope.isGraphMode = function(graphName) {
      return $scope.paragraph &&
        $scope.paragraph.resultType() === PResultType.TABLE &&
        $scope.paragraph.graphMode() === graphName;
    };

    $scope.onGraphOptionChange = function() {
      clearUnknownColsFromGraphOption();
      var mode = $scope.paragraph.config.graph.mode;
      $scope.setGraphMode(mode, true, false);
    };

    $scope.removeGraphOptionKeys = function(idx) {
      $scope.paragraph.config.graph.keys.splice(idx, 1);
      clearUnknownColsFromGraphOption();
      var mode = $scope.paragraph.config.graph.mode;
      $scope.setGraphMode(mode, true, false);
    };

    $scope.removeGraphOptionValues = function(idx) {
      $scope.paragraph.config.graph.values.splice(idx, 1);
      clearUnknownColsFromGraphOption();
      var mode = $scope.paragraph.config.graph.mode;
      $scope.setGraphMode(mode, true, false);
    };

    $scope.removeGraphOptionGroups = function(idx) {
      $scope.paragraph.config.graph.groups.splice(idx, 1);
      clearUnknownColsFromGraphOption();
      var mode = $scope.paragraph.config.graph.mode;
      $scope.setGraphMode(mode, true, false);
    };

    $scope.setGraphOptionValueAggr = function(idx, aggr) {
      $scope.paragraph.config.graph.values[idx].aggr = aggr;
      clearUnknownColsFromGraphOption();
      var mode = $scope.paragraph.config.graph.mode;
      $scope.setGraphMode(mode, true, false);
    };

    /* Clear unknown columns from graph option */
    var clearUnknownColsFromGraphOption = function() {
      var unique = function(list) {
        for (var i = 0; i < list.length; i++) {
          for (var j = i + 1; j < list.length; j++) {
            if (angular.equals(list[i], list[j])) {
              list.splice(j, 1);
            }
          }
        }
      };

      var removeUnknown = function(list) {
        for (var i = 0; i < list.length; i++) {
          // remove non existing column
          var found = false;
          for (var j = 0; j < $scope.paragraph.result.columnNames.length; j++) {
            var a = list[i];
            var b = $scope.paragraph.result.columnNames[j];
            if (a.index === b.index && a.name === b.name) {
              found = true;
              break;
            }
          }
          if (!found) {
            list.splice(i, 1);
          }
        }
      };

      unique($scope.paragraph.config.graph.keys);
      removeUnknown($scope.paragraph.config.graph.keys);

      removeUnknown($scope.paragraph.config.graph.values);

      unique($scope.paragraph.config.graph.groups);
      removeUnknown($scope.paragraph.config.graph.groups);
    };

    /* select default key and value if there're none selected */
    var selectDefaultColsForGraphOption = function() {
      if ($scope.paragraph.config.graph.keys.length === 0 && $scope.paragraph.result.columnNames.length > 0) {
        $scope.paragraph.config.graph.keys.push($scope.paragraph.result.columnNames[0]);
      }

      if ($scope.paragraph.config.graph.values.length === 0 && $scope.paragraph.result.columnNames.length > 1) {
        $scope.paragraph.config.graph.values.push($scope.paragraph.result.columnNames[1]);
      }
    };

    var pivot = function(data) {
      var keys = $scope.paragraph.config.graph.keys;
      var groups = $scope.paragraph.config.graph.groups;
      var values = $scope.paragraph.config.graph.values;

      var aggrFunc = {
        sum : function(a,b) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return varA + varB;
        },
        count : function(a,b) {
          var varA = (a !== undefined) ? parseInt(a) : 0;
          var varB = (b !== undefined) ? 1 : 0;
          return varA + varB;
        },
        min : function(a,b) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return Math.min(varA,varB);
        },
        max : function(a,b) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return Math.max(varA,varB);
        },
        avg : function(a,b,c) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return varA + varB;
        }
      };

      var aggrFuncDiv = {
        sum : false,
        count : false,
        min : false,
        max : false,
        avg : true
      };

      var schema = {};
      var rows = {};

      for (var i=0; i < data.rows.length; i++) {
        var row = data.rows[i];
        var newRow = {};
        var s = schema;
        var p = rows;

        for (var k=0; k < keys.length; k++) {
          var key = keys[k];

          // add key to schema
          if (!s[key.name]) {
            s[key.name] = {
               order : k,
               index : key.index,
               type : 'key',
               children : {}
            };
          }
          s = s[key.name].children;

          // add key to row
          var keyKey = row[key.index];
          if (!p[keyKey]) {
            p[keyKey] = {};
          }
          p = p[keyKey];
        }

        for (var g=0; g < groups.length; g++) {
          var group = groups[g];
          var groupKey = row[group.index];

          // add group to schema
          if (!s[groupKey]) {
            s[groupKey] = {
               order : g,
               index : group.index,
               type : 'group',
               children : {}
            };
          }
          s = s[groupKey].children;

          // add key to row
          if (!p[groupKey]) {
            p[groupKey] = {};
          }
          p = p[groupKey];
        }

        for (var v=0; v < values.length; v++) {
          var value = values[v];
          var valueKey = value.name + '(' + value.aggr + ')';

          // add value to schema
          if (!s[valueKey]) {
            s[valueKey] = {
              type : 'value',
              order : v,
              index : value.index
            };
          }

          // add value to row
          if (!p[valueKey]) {
            p[valueKey] = {
                value : (value.aggr !== 'count') ? row[value.index] : 1,
                count: 1
            };
          } else {
            p[valueKey] = {
                value : aggrFunc[value.aggr](p[valueKey].value, row[value.index], p[valueKey].count + 1),
                count : (aggrFuncDiv[value.aggr]) ?  p[valueKey].count + 1 : p[valueKey].count
            };
          }
        }
      }
      //console.log('schema=%o, rows=%o', schema, rows);

      return {
        schema : schema,
        rows : rows
      };
    };

    var pivotDataToD3ChartFormat = function(data, allowTextXAxis, fillMissingValues?, chartType?) {
      // construct d3 data
      var d3g = [];

      var schema = data.schema;
      var rows = data.rows;
      var values = $scope.paragraph.config.graph.values;

      var concat = function(o, n) {
        if (!o) {
          return n;
        } else {
          return o + '.' + n;
        }
      };

      var getSchemaUnderKey = function(key, s) {
        for (var c in key.children) {
          s[c] = {};
          getSchemaUnderKey(key.children[c], s[c]);
        }
      };

      var traverse = function(sKey, s, rKey, r, func, rowName?, rowValue?, colName?) {
        //console.log('TRAVERSE sKey=%o, s=%o, rKey=%o, r=%o, rowName=%o, rowValue=%o, colName=%o', sKey, s, rKey, r, rowName, rowValue, colName);

        if (s.type === 'key') {
          rowName = concat(rowName, sKey);
          rowValue = concat(rowValue, rKey);
        } else if (s.type === 'group') {
          colName = concat(colName, rKey);
        } else if (s.type === 'value' && sKey === rKey || valueOnly) {
          colName = concat(colName, rKey);
          func(rowName, rowValue, colName, r);
        }

        for (var c in s.children) {
          if (fillMissingValues && s.children[c].type === 'group' && r[c] === undefined) {
            var cs = {};
            getSchemaUnderKey(s.children[c], cs);
            traverse(c, s.children[c], c, cs, func, rowName, rowValue, colName);
            continue;
          }

          for (var j in r) {
            if (s.children[c].type === 'key' || c === j) {
              traverse(c, s.children[c], j, r[j], func, rowName, rowValue, colName);
            }
          }
        }
      };

      var keys = $scope.paragraph.config.graph.keys;
      var groups = $scope.paragraph.config.graph.groups;
      var values = $scope.paragraph.config.graph.values;
      var valueOnly = (keys.length === 0 && groups.length === 0 && values.length > 0);
      var noKey = (keys.length === 0);
      var isMultiBarChart = (chartType === GraphMode.multiBarChart);

      var sKey = Object.keys(schema)[0];

      var rowNameIndex = {};
      var rowIdx = 0;
      var colNameIndex = {};
      var colIdx = 0;
      var rowIndexValue = {};

      for (var k in rows) {
        traverse(sKey, schema[sKey], k, rows[k], function(rowName, rowValue, colName, value) {
          //console.log('RowName=%o, row=%o, col=%o, value=%o', rowName, rowValue, colName, value);
          if (rowNameIndex[rowValue] === undefined) {
            rowIndexValue[rowIdx] = rowValue;
            rowNameIndex[rowValue] = rowIdx++;
          }

          if (colNameIndex[colName] === undefined) {
            colNameIndex[colName] = colIdx++;
          }
          var i = colNameIndex[colName];
          if (noKey && isMultiBarChart) {
            i = 0;
          }

          if (!d3g[i]) {
            d3g[i] = {
              values : [],
              key : (noKey && isMultiBarChart) ? 'values' : colName
            };
          }

          var xVar = isNaN(rowValue) ? ((allowTextXAxis) ? rowValue : rowNameIndex[rowValue]) : parseFloat(rowValue);
          var yVar = 0;
          if (xVar === undefined) { xVar = colName; }
          if (value !== undefined) {
            yVar = isNaN(value.value) ? 0 : parseFloat(value.value) / parseFloat(value.count);
          }
          d3g[i].values.push({
            x : xVar,
            y : yVar
          });
        });
      }

      // clear aggregation name, if possible
      var namesWithoutAggr = {};
      // TODO - This part could use som refactoring - Weird if/else with similar actions and variable names
      for (var colName in colNameIndex) {
        var withoutAggr = colName.substring(0, colName.lastIndexOf('('));
        if (!namesWithoutAggr[withoutAggr]) {
          namesWithoutAggr[withoutAggr] = 1;
        } else {
          namesWithoutAggr[withoutAggr]++;
        }
      }

      if (valueOnly) {
        for (var valueIndex = 0; valueIndex < d3g[0].values.length; valueIndex++) {
          var colName = d3g[0].values[valueIndex].x;
          if (!colName) {
            continue;
          }

          var withoutAggr = colName.substring(0, colName.lastIndexOf('('));
          if (namesWithoutAggr[withoutAggr] <= 1 ) {
            d3g[0].values[valueIndex].x = withoutAggr;
          }
        }
      } else {
        for (var d3gIndex = 0; d3gIndex < d3g.length; d3gIndex++) {
          var colName = d3g[d3gIndex].key;
          var withoutAggr = colName.substring(0, colName.lastIndexOf('('));
          if (namesWithoutAggr[withoutAggr] <= 1 ) {
            d3g[d3gIndex].key = withoutAggr;
          }
        }

        // use group name instead of group.value as a column name, if there're only one group and one value selected.
        if (groups.length === 1 && values.length === 1) {
          for (d3gIndex = 0; d3gIndex < d3g.length; d3gIndex++) {
            var colName = d3g[d3gIndex].key;
            colName = colName.split('.')[0];
            d3g[d3gIndex].key = colName;
          }
        }
      }

      return {
        xLabels : rowIndexValue,
        d3g : d3g
      };
    };

    $scope.setGraphHeight = function() {
      var height = $('#p' + $scope.paragraph.id + '_graph').height();

      var newParams = angular.copy($scope.paragraph.settings.params);
      var newConfig = angular.copy($scope.paragraph.config);

      newConfig.graph.height = height;

      commitParagraph(newConfig, newParams);
    };

    /** Utility function */
    if (typeof String.prototype.startsWith !== 'function') {
      String.prototype.startsWith = function(str) {
        return this.slice(0, str.length) === str;
      };
    }

    $scope.goToSingleParagraph = function () {
      var noteId = $route.current.pathParams.noteId;
      var redirectToUrl = location.protocol + '//' + location.host + '/#/notebook/' + noteId + '/paragraph/' + $scope.paragraph.id + '?asIframe';
      $window.open(redirectToUrl);
    };
  });
}
