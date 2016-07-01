import logging

from .util import Recommender_wrapper,Recommender,Extension,Extension_wrapper

__version__ = '2.0.1'

class NullHandler(logging.Handler):
    def emit(self, record):
        pass

logger = logging.getLogger(__name__)
if len(logger.handlers) == 0:# To ensure reload() doesn't add another one
    logger.addHandler(NullHandler())
