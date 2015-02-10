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
 * @name zeppelinWebApp.controller:ParagraphEditorCtrl
 * @description
 * # ParagraphEditorCtrl
 * Controller of the paragraph editor
 */

module zeppelin {

  interface IParagraphEditorCtrlScope extends ng.IScope {
    editor: any;

    paragraphFocused: boolean;

    aceChanged: () => void;
    aceLoaded: (editor: any) => void;
    handleFocus: (value: any) => void;

    $parent: IParagraphCtrlScope;
  }

  angular.module('zeppelinWebApp').controller('ParagraphEditorCtrl', function(
    $scope: IParagraphEditorCtrlScope,
    $rootScope: IZeppelinRootScope,
    $timeout: ng.ITimeoutService) {

    var editorMode = {scala: 'ace/mode/scala', sql: 'ace/mode/sql', markdown: 'ace/mode/markdown'};
    $scope.paragraphFocused = false;

    $scope.$on('focusParagraph', function(event, paragraphId) {
      if ($scope.$parent.paragraph.id === paragraphId) {
        $scope.editor.focus();
      }
    });

    var setEditorHeight = function(id, height) {
      $('#' + id).height(height.toString() + 'px');
    };

    $scope.aceChanged = function() {
      $scope.$parent.editingText = $scope.editor.getSession().getValue();
    };

    $scope.aceLoaded = function(_editor) {
      var langTools = ace.require('ace/ext/language_tools');
      var Range = ace.require('ace/range').Range;

      $scope.editor = _editor;
      if (_editor.container.id !== '{{$parent.paragraph.id}}_editor') {
        $scope.editor.renderer.setShowGutter(false);
        $scope.editor.setHighlightActiveLine(false);
        $scope.editor.focus();
        var hight = $scope.editor.getSession().getScreenLength() * $scope.editor.renderer.lineHeight + $scope.editor.renderer.scrollBar.getWidth();
        setEditorHeight(_editor.container.id, hight);

        $scope.editor.getSession().setUseWrapMode(true);
        if (navigator.appVersion.indexOf('Mac') !== -1 ) {
          $scope.editor.setKeyboardHandler('ace/keyboard/emacs');
        } else if (navigator.appVersion.indexOf('Win') !== -1 ||
          navigator.appVersion.indexOf('X11') !== -1 ||
          navigator.appVersion.indexOf('Linux') !== -1) {
          // not applying emacs key binding while the binding override Ctrl-v. default behavior of paste text on windows.
        }

        $scope.editor.setOptions({
          enableBasicAutocompletion: true,
          enableSnippets: false,
          enableLiveAutocompletion:false
        });
        var remoteCompleter = {
          getCompletions : function(editor, session, pos, prefix, callback) {
            if (!$scope.editor.isFocused() ){ return;}

            var buf = session.getTextRange(new Range(0, 0, pos.row, pos.column));
            $rootScope.sendEventToServer(new ZCodeCompletionEvent($scope.$parent.paragraph, buf));

            $scope.$on('completionList', function(event, data) {
              if (data.completions) {
                var completions = [];
                for (var c in data.completions) {
                  var v = data.completions[c];
                  completions.push({
                    name:v,
                    value:v,
                    score:300
                  });
                }
                callback(null, completions);
              }
            });
          }
        };
        langTools.addCompleter(remoteCompleter);

        $scope.handleFocus = function(value) {
          $scope.paragraphFocused = value;
          // Protect against error in case digest is already running
          $timeout(function() {
            // Apply changes since they come from 3rd party library
            $scope.$digest();
          });
        };

        $scope.editor.on('focus', function() {
          $scope.handleFocus(true);
        });

        $scope.editor.on('blur', function() {
          $scope.handleFocus(false);
        });

        $scope.editor.getSession().on('change', function(e, editSession) {
          hight = editSession.getScreenLength() * $scope.editor.renderer.lineHeight + $scope.editor.renderer.scrollBar.getWidth();
          setEditorHeight(_editor.container.id, hight);
          $scope.editor.resize();
        });


        var code = $scope.editor.getSession().getValue();
        if (String(code).startsWith('%sql')) {
          $scope.editor.getSession().setMode(editorMode.sql);
        } else if ( String(code).startsWith('%md')) {
          $scope.editor.getSession().setMode(editorMode.markdown);
        } else {
          $scope.editor.getSession().setMode(editorMode.scala);
        }

        $scope.editor.commands.addCommand({
          name: 'run',
          bindKey: {win: 'Shift-Enter', mac: 'Shift-Enter'},
          exec: function(editor) {
            var editorValue = editor.getValue();
            if (editorValue) {
              $scope.$parent.runParagraph(editorValue);
            }
          },
          readOnly: false
        });

        // autocomplete on '.'
        /*
         $scope.editor.commands.on('afterExec', function(e, t) {
         if (e.command.name == 'insertstring' && e.args == '.' ) {
         var all = e.editor.completers;
         //e.editor.completers = [remoteCompleter];
         e.editor.execCommand('startAutocomplete');
         //e.editor.completers = all;
         }
         });
         */

        // autocomplete on 'ctrl + .'
        $scope.editor.commands.bindKey('ctrl-.', 'startAutocomplete');
        $scope.editor.commands.bindKey('ctrl-space', null);

        // handle cursor moves
        $scope.editor.keyBinding.origOnCommandKey = $scope.editor.keyBinding.onCommandKey;
        $scope.editor.keyBinding.onCommandKey = function(e, hashId, keyCode) {
          if ($scope.editor.completer && $scope.editor.completer.activated) { // if autocompleter is active
          } else {
            var numRows;
            var currentRow;
            if (keyCode === 38 || (keyCode === 80 && e.ctrlKey)) {  // UP
              numRows = $scope.editor.getSession().getLength();
              currentRow = $scope.editor.getCursorPosition().row;
              if (currentRow === 0) {
                // move focus to previous paragraph
                $rootScope.$emit('moveFocusToPreviousParagraph', $scope.$parent.paragraph.id);
              }
            } else if (keyCode === 40 || (keyCode === 78 && e.ctrlKey)) {  // DOWN
              numRows = $scope.editor.getSession().getLength();
              currentRow = $scope.editor.getCursorPosition().row;
              if (currentRow === numRows-1) {
                // move focus to next paragraph
                $rootScope.$emit('moveFocusToNextParagraph', $scope.$parent.paragraph.id);
              }
            }
          }
          this.origOnCommandKey(e, hashId, keyCode);
        };
      }
    };

  });
}
