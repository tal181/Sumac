package com.quantifind.sumac

import collection.Map

/**
 * Some external source of config information.  It both allows config to be read from, and saved to, some external
 * format.
 */
trait ExternalConfig {

  /**
   * Read config from an external source, and use that to modify the set of arguments.  The "original" arguments
   * are given as an argument to this function, so that this method can base its behavior on those arguments.  (Eg.,
   * it can take a filename from those args.)  It should return the complete set of args that should be used.  It is
   * free to choose to completely replace the original args, append to them, modify them, etc.
   *
   * in general, implementations should be abstract override, to allow multiple external configs
   * via the Stackable Trait Pattern
   */
  def readArgs(originalArgs: Map[String,String]): Map[String,String]

  /**
   * save the config back to the external source.  Any parameters for this method should have already been extracted
   * from the call to readArgs
   *
   * as with readArgs, this should in general be implemented as an abstract override
   */
  def saveConfig(): Unit
}
