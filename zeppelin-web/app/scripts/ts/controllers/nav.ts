/* global $:false */
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
 * @name zeppelinWebApp.controller:NavCtrl
 * @description
 * # NavCtrl
 * Controller of the top navigation, mainly use for the dropdown menu
 *
 * @author anthonycorbacho
 */
module zeppelin {
  interface INavCtrlScope extends ng.IScope {
    notes: Array<Notebook>;

    createNewNote: () => void;
    isActive: (noteId: string) => boolean;
  }

  zeppelinWebApp.controller('NavCtrl', function(
    $scope: INavCtrlScope,
    $rootScope: IZeppelinRootScope,
    $routeParams) {
    /** Current list of notes (ids) */

    $('#notebook-list').perfectScrollbar({suppressScrollX: true});

    /** Set the new menu */
    $scope.$on('setNoteMenu', function(event, notes) {
        $scope.notes = notes;
    });

    var loadNotes = function() {
      var e = new ZListNotesEvent();
      $rootScope.sendEventToServer(e);
    };
    loadNotes();

    /** Create a new note */
    $scope.createNewNote = function() {
      $rootScope.sendEventToServer(new ZNewNoteEvent());
    };

    /** Check if the note url is equal to the current note */
    $scope.isActive = function(noteId) {
      if ($routeParams.noteId === noteId) {
        return true;
      }
      return false;
    };
  });
}
