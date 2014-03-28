/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.r.service;


public class NoSuchRCommandException extends RuntimeException {

  private final String id;

  public NoSuchRCommandException(String id) {
    super("No such R command with ID: " + id);
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
